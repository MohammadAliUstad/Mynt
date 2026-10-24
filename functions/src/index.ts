import {onCall, HttpsError} from "firebase-functions/v2/https";
import {setGlobalOptions} from "firebase-functions";
import * as logger from "firebase-functions/logger";
import axios from "axios";
import FormData from "form-data";

setGlobalOptions({maxInstances: 10});

const SONIOX_BASE = "https://api.soniox.com/v1";
const SONIOX_MODEL = "stt-async-v4";

interface TranscribeVoiceData {
  audioBase64?: string;
  base64Audio?: string;
  mimeType?: string;
}

function pcmToWav(pcmBuffer: Buffer, sampleRate = 16000, numChannels = 1, bitsPerSample = 16): Buffer {
  const header = Buffer.alloc(44);
  const dataSize = pcmBuffer.length;
  const byteRate = (sampleRate * numChannels * bitsPerSample) / 8;
  const blockAlign = (numChannels * bitsPerSample) / 8;

  header.write("RIFF", 0);
  header.writeUInt32LE(36 + dataSize, 4);
  header.write("WAVE", 8);
  header.write("fmt ", 12);
  header.writeUInt32LE(16, 16);
  header.writeUInt16LE(1, 20);
  header.writeUInt16LE(numChannels, 22);
  header.writeUInt32LE(sampleRate, 24);
  header.writeUInt32LE(byteRate, 28);
  header.writeUInt16LE(blockAlign, 32);
  header.writeUInt16LE(bitsPerSample, 34);
  header.write("data", 36);
  header.writeUInt32LE(dataSize, 40);

  return Buffer.concat([header, pcmBuffer]);
}

const sleep = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

export const transcribeVoice = onCall<TranscribeVoiceData>({
  cors: true,
  secrets: ["SONIOX_API_KEY", "GEMINI_API_KEY"],
  timeoutSeconds: 90,
}, async (request) => {
  const rawBase64 = request.data.audioBase64 || request.data.base64Audio;

  if (!rawBase64) {
    throw new HttpsError("invalid-argument", "No audio data provided.");
  }

  const activeApiKey = process.env.SONIOX_API_KEY;

  if (!activeApiKey) {
    throw new HttpsError("failed-precondition", "Soniox API key is not configured in Secret Manager.");
  }

  const authHeaders = {Authorization: `Bearer ${activeApiKey}`};
  let fileId: string | undefined;
  let transcriptionId: string | undefined;

  try {
    const rawPcmBuffer = Buffer.from(rawBase64, "base64");
    const wavBuffer = pcmToWav(rawPcmBuffer, 16000, 1, 16);

    const form = new FormData();
    form.append("file", wavBuffer, {
      filename: "speech.wav",
      contentType: "audio/wav",
    });

    const uploadRes = await axios.post(`${SONIOX_BASE}/files`, form, {
      headers: {...form.getHeaders(), ...authHeaders},
      timeout: 20000,
    });
    fileId = uploadRes.data.id;

    const createRes = await axios.post(
        `${SONIOX_BASE}/transcriptions`,
        {model: SONIOX_MODEL, file_id: fileId},
        {headers: authHeaders, timeout: 15000}
    );
    transcriptionId = createRes.data.id;

    let status = createRes.data.status;
    for (let i = 0; i < 45 && status !== "completed"; i++) {
      if (status === "error" || status === "failed") {
        throw new Error(`Soniox transcription failed: ${JSON.stringify(createRes.data)}`);
      }
      await sleep(1000);
      const statusRes = await axios.get(`${SONIOX_BASE}/transcriptions/${transcriptionId}`, {
        headers: authHeaders,
        timeout: 15000,
      });
      status = statusRes.data.status;
      if (status === "error" || status === "failed") {
        throw new Error(
            `Soniox transcription failed: ${statusRes.data.error_type || ""} ${statusRes.data.error_message || ""}`
        );
      }
    }

    if (status !== "completed") {
      throw new Error("Soniox transcription timed out.");
    }

    const transcriptRes = await axios.get(`${SONIOX_BASE}/transcriptions/${transcriptionId}/transcript`, {
      headers: authHeaders,
      timeout: 15000,
    });

    const data = transcriptRes.data;
    const textResult: string =
        data.text || (data.tokens ? data.tokens.map((t: any) => t.text).join("") : "");

    return {
      success: true,
      text: textResult.trim(),
    };
  } catch (error: any) {
    logger.error("Soniox failed", {
      status: error?.response?.status,
      data: error?.response?.data,
      url: error?.config?.url,
      message: error?.message,
    });

    const errorMessage = error?.response?.data?.message || error?.message || "Failed to transcribe audio via Soniox.";
    throw new HttpsError("internal", errorMessage);
  } finally {
    if (transcriptionId) {
      axios.delete(`${SONIOX_BASE}/transcriptions/${transcriptionId}`, {headers: authHeaders}).catch(() => {});
    }
    if (fileId) {
      axios.delete(`${SONIOX_BASE}/files/${fileId}`, {headers: authHeaders}).catch(() => {});
    }
  }
});

// ===================== Gemini: parse transactions & split bills =====================

const GEMINI_BASE = "https://generativelanguage.googleapis.com/v1beta";
const GEMINI_MODEL = "gemini-3.8-flash";

const EXPENSE_CATEGORIES = [
  "Food", "Groceries", "Travel", "Shopping", "Bills", "Rent",
  "Health", "Entertainment", "Education", "EMI", "Other",
];
const INCOME_CATEGORIES = [
  "Salary", "Freelance", "Business", "Gift", "Investment", "Refund", "Other",
];

interface CandidateTransaction {
  id: string;
  title: string;
  amount: number;
  category: string;
  date: string;
  type: string;
  personName?: string;
  note?: string;
}

interface ParseTransactionsData {
  text?: string;
  today?: string;
  recentTransactions?: CandidateTransaction[];
  activeParticipants?: string[];
}

const PARSER_INSTRUCTIONS = `You are an ultra-smart personal finance voice parser for an app called Mynt. You understand English, Hindi (Devanagari), and Hinglish seamlessly.

LANGUAGE & NUMBER RULES:
- Understand numbers spoken as words in any language ("इक्यावन हज़ार पाँच सौ", "ikyavan hazaar paanch sau", "fifty one thousand five hundred" = 51500).
- Convert relative dates ("1st October", "kal" = yesterday, "parso" = day before yesterday) to YYYY-MM-DD using "Today".
- Always output English values: categories from the allowed lists, and capitalized names in Latin script.

===================================================
TITLE vs. NOTE RULE (CRITICAL FOR ALL TRANSACTIONS):
===================================================
Every transaction has a "title" (shown in the main list) and a "note" (shown as an AI insight sentence on the detail screen and used for search):
1. "title" MUST be a short, clean 1-to-2 word noun naming the core item only. NEVER include verbs, locations, or people in the title!
   - User says "Bought a chair online for 2300" -> title = "Chair", category = "Shopping" (NEVER "Bought a chair online").
   - User says "Spent 50 rupees on coffee in office with a colleague" -> title = "Coffee", category = "Food".
   - User says "Paid 450 for cab to airport" -> title = "Cab", category = "Travel".
2. "note" MUST be a clean, natural sentence describing what the AI understood about the context (where, how, with whom, or why), written in a helpful summary style (not a raw transcript):
   - For "Bought a chair online for 2300" -> note = "This chair was purchased online."
   - For "Spent 50 rupees on coffee in office with a colleague" -> note = "This coffee was spent in the office with a colleague."
   - For "Paid 450 for cab to airport" -> note = "This cab was taken to the airport."
   - If the user gave NO extra context (e.g., just "Spent 20 on tea"), set note = "".

===================================================
SMART CLARIFICATION & AMBIGUITY RULES (HIGHEST PRIORITY):
===================================================
Note: If the user's text ends with a follow-up answer (like ". Split bill", ". Regular expense", ". No, save without name", or a person's name), use that follow-up to resolve the rule and DO NOT ask again!

1. MISSING AMOUNT:
If the user mentions an expense, income, lent, borrowed, or split bill, but forgets to state the amount (or amount is 0):
- Return entries = [] and set missingInfo = "How much was the amount?"

2. LENT / BORROWED MISSING PERSON NAME:
If the user says they lent or borrowed money (e.g., "I lent 2000 for rent" or "I borrowed 500"), and NO person's name is mentioned:
- Check if the user already answered to skip the name (e.g., text contains "save without name", "without name", "skip", or ends with ". No").
- If they have NOT answered yet: Return entries = [] and set missingInfo to EXACTLY:
  - For LENT: "Do you want to give the name of the person that you lent the money to?"
  - For BORROWED: "Do you want to give the name of the person that you borrowed the money from?"
- If they answered "No" / "Save without name": Create the LENT/BORROWED entry with personName = "", title = "Lent" (or the reason like "Rent"), and note = "Lent for rent without a specified name."
- If they answered with a name (e.g., "I borrowed 2000 for rent. Rahul"): Create the entry with personName = "Rahul", title = "Rent", and note = "Borrowed from Rahul for rent."

3. AMBIGUOUS GROUP EXPENSE vs. REGULAR EXPENSE:
If the user mentions spending money involving other people (e.g., "Spent 50 on coffee in office with a colleague", "Had dinner with a friend for 400") where it is NOT explicitly clear whether they want to split the bill or just log a personal expense:
- Check if it's an obvious split bill (mentions "split", "shares", "paid by X", "each"). If obvious, go to SPLIT BILL RULE.
- Check if the user already clarified at the end of the text (e.g., text ends with ". Split bill", ". Split", ". Regular expense", or ". Expense").
- If NOT clarified yet: Return entries = [] and set missingInfo to EXACTLY:
  "Is this a split bill, or just a regular expense?"
- If clarified as "Regular expense" (or "Expense"): Create a regular EXPENSE entry following the TITLE vs. NOTE RULE (e.g., title = "Coffee", category = "Food", note = "This coffee was spent in the office with a colleague.").
- If clarified as "Split bill" (or "Split"): Follow the SPLIT BILL RULE below. If no specific friend names were given (e.g., "with a colleague"), use participants = ["You", "Colleague"].

===================================================
SPLIT BILL RULE:
===================================================
If the user describes a confirmed group expense shared between people:
1. Set action = "SPLIT_BILL" and type = "SPLIT_BILL".
2. Set title = short 1-2 word item name (e.g., "Lunch", "Coffee", "Dinner").
3. Set amount = total bill amount.
4. Set paidBy = the person who paid ("You" if unspecified or if the speaker paid).
5. Set participants = ALL people involved, including "You".
6. Set excluded = ONLY people who didn't share the cost. Otherwise [].

===================================================
CRITICAL UPDATE RULE:
===================================================
If the user wants to edit/correct an existing transaction (e.g., "change 1st October salary to 52,500", "make changes in the previous transaction"):
1. Inspect "Recent Transactions for matching" (newest first).
2. Set action = "UPDATE", id = exact candidate "id", and update the requested fields while keeping the rest intact.

===================================================
CATEGORIES FOR NEW TRANSACTIONS:
===================================================
- For LENT: Category = "Lending".
- For BORROWED: Category = "Borrowing".
- For INCOME: Category from ${INCOME_CATEGORIES.join(", ")}.
- For EXPENSE: Category from ${EXPENSE_CATEGORIES.join(", ")}.
`;

const PARSER_SCHEMA = {
  type: "OBJECT",
  properties: {
    entries: {
      type: "ARRAY",
      items: {
        type: "OBJECT",
        properties: {
          action: {type: "STRING", enum: ["CREATE", "UPDATE", "SPLIT_BILL", "CLARIFY"]},
          id: {type: "STRING"},
          type: {type: "STRING", enum: ["EXPENSE", "INCOME", "LENT", "BORROWED", "SPLIT_BILL"]},
          title: {type: "STRING"},
          note: {type: "STRING"},
          personName: {type: "STRING"},
          amount: {type: "NUMBER"},
          category: {type: "STRING"},
          date: {type: "STRING"},
          paidBy: {type: "STRING"},
          participants: {
            type: "ARRAY",
            items: {type: "STRING"},
          },
          excluded: {
            type: "ARRAY",
            items: {type: "STRING"},
          },
        },
        required: ["action", "type", "title", "amount", "date"],
      },
    },
    missingInfo: {type: "STRING"},
  },
  required: ["entries"],
};

export const parseTransactions = onCall<ParseTransactionsData>({
  cors: true,
  secrets: ["GEMINI_API_KEY"],
  timeoutSeconds: 60,
}, async (request) => {
  const spokenText = (request.data.text || "").trim();
  if (!spokenText) {
    throw new HttpsError("invalid-argument", "No text provided.");
  }

  const geminiKey = process.env.GEMINI_API_KEY;
  if (!geminiKey) {
    throw new HttpsError("failed-precondition", "Gemini API key is not configured in Secret Manager.");
  }

  const dateRegex = /^\d{4}-\d{2}-\d{2}$/;
  const today = request.data.today && dateRegex.test(request.data.today) ?
      request.data.today :
      new Date().toISOString().slice(0, 10);

  const candidateList = request.data.recentTransactions || [];
  const candidateContext = candidateList.length > 0 ?
      `\nRecent Transactions for matching:\n${JSON.stringify(candidateList)}` :
      "";

  const participants = Array.isArray(request.data.activeParticipants) ?
      request.data.activeParticipants.map((p) => String(p).trim()).filter(Boolean) :
      [];
  const participantContext = participants.length > 0 ?
      `\nActive participants already in the split session: ${participants.join(", ")}` :
      "";

  try {
    const response = await axios.post(
        `${GEMINI_BASE}/models/${GEMINI_MODEL}:generateContent`,
        {
          systemInstruction: {parts: [{text: PARSER_INSTRUCTIONS}]},
          contents: [{
            role: "user",
            parts: [{text: `Today: ${today}${candidateContext}${participantContext}\nSpoken command: "${spokenText}"`}],
          }],
          generationConfig: {
            temperature: 0,
            responseMimeType: "application/json",
            responseSchema: PARSER_SCHEMA,
          },
        },
        {
          headers: {"x-goog-api-key": geminiKey, "Content-Type": "application/json"},
          timeout: 40000,
        }
    );

    const parts = response.data?.candidates?.[0]?.content?.parts || [];
    const rawText: string = parts.map((p: any) => p.text || "").join("");
    const cleaned = rawText.replace(/```json|```/g, "").trim();
    const parsed = JSON.parse(cleaned);

    const missingInfo = String(parsed.missingInfo || "").trim();
    if (missingInfo) {
      return {success: true, action: "CLARIFY", missingInfo, entries: []};
    }

    const entries = (Array.isArray(parsed.entries) ? parsed.entries : [])
        .map((e: any) => {
          const rawAction = String(e.action || "").toUpperCase();
          const rawType = String(e.type || "").toUpperCase();

          if (rawAction === "SPLIT_BILL" || rawType === "SPLIT_BILL") {
            return {
              action: "SPLIT_BILL",
              type: "SPLIT_BILL",
              title: String(e.title || "Split Bill").trim(),
              amount: Number(e.amount),
              date: dateRegex.test(String(e.date)) ? String(e.date) : today,
              paidBy: String(e.paidBy || "You").trim(),
              participants: Array.isArray(e.participants) ? e.participants.map((p: any) => String(p).trim()) : ["You"],
              excluded: Array.isArray(e.excluded) ? e.excluded.map((p: any) => String(p).trim()) : [],
            };
          }

          const action = (rawAction === "UPDATE" || rawAction === "CLARIFY") ? rawAction : "CREATE";
          const type = (rawType === "LENT" || rawType === "BORROWED" || rawType === "INCOME") ? rawType : "EXPENSE";

          let category = "Other";
          if (type === "LENT") {
            category = "Lending";
          } else if (type === "BORROWED") {
            category = "Borrowing";
          } else if (type === "INCOME") {
            category = INCOME_CATEGORIES.find((c) => c.toLowerCase() === String(e.category).toLowerCase()) || "Other";
          } else {
            category = EXPENSE_CATEGORIES.find((c) => c.toLowerCase() === String(e.category).toLowerCase()) || "Other";
          }

          const personName = String(e.personName || "").trim();
          const title = String(e.title || (personName ? personName : category)).trim();
          const note = String(e.note || "").trim();

          return {
            action,
            id: String(e.id || "").trim(),
            type,
            title,
            note,
            personName,
            amount: Number(e.amount),
            category,
            date: dateRegex.test(String(e.date)) ? String(e.date) : today,
          };
        })
        .filter((e: any) => Number.isFinite(e.amount) && e.amount > 0);

    return {success: true, entries};
  } catch (error: any) {
    logger.error("Gemini parse failed", {
      status: error?.response?.status,
      data: error?.response?.data,
      message: error?.message,
    });
    throw new HttpsError("internal", error?.response?.data?.error?.message || error?.message || "Parsing failed.");
  }
});

// ===================== Gemini: settle-up message =====================

interface SettleBill {
  title?: string;
  amount?: number;
  paidBy?: string;
  date?: string;
}

interface SettleTransfer {
  from?: string;
  to?: string;
  amount?: number;
}

interface GenerateSettleMessageData {
  settledOn?: string;
  currencySymbol?: string;
  people?: string[];
  bills?: SettleBill[];
  transfers?: SettleTransfer[];
}

const SETTLE_MESSAGE_INSTRUCTIONS = `You write a short, friendly message that the user will send to their friends in a group chat after settling a shared-expense split.

RULES:
- "You" in the data is the user, who is the sender. Write in first person: say "I" or "me" instead of "You".
- Use ONLY the numbers and names given. Never recalculate or invent amounts, names, dates or places.
- Structure: (1) a one-line friendly opener, (2) a short recap of the bills: title, amount and who paid, (3) the final settle-up with one line per transfer, like "Arbaz pays me 120", (4) a short friendly closing line asking everyone to pay up and confirm once done.
- If there are no transfers, say everyone is already square and thank them.
- Plain text only. No markdown and no asterisks. Use at most 3 emojis.
- Keep it under 170 words. English only.
- Write the currency symbol directly before every amount, with thousands separators.
`;

export const generateSettleMessage = onCall<GenerateSettleMessageData>({
  cors: true,
  secrets: ["GEMINI_API_KEY"],
  timeoutSeconds: 60,
}, async (request) => {
  const geminiKey = process.env.GEMINI_API_KEY;
  if (!geminiKey) {
    throw new HttpsError("failed-precondition", "Gemini API key is not configured in Secret Manager.");
  }

  const d = request.data || {};
  const bills = (Array.isArray(d.bills) ? d.bills : []).slice(0, 50);
  const transfers = (Array.isArray(d.transfers) ? d.transfers : []).slice(0, 50);

  if (bills.length === 0 && transfers.length === 0) {
    throw new HttpsError("invalid-argument", "Nothing to summarise.");
  }

  const clean = (v: any) => String(v ?? "").trim().slice(0, 80);
  const total = bills.reduce((sum, b) => sum + (Number(b.amount) || 0), 0);

  const payload = {
    settledOn: clean(d.settledOn),
    currencySymbol: clean(d.currencySymbol).slice(0, 4),
    people: (Array.isArray(d.people) ? d.people : []).map(clean).slice(0, 30),
    totalSpent: Math.round(total * 100) / 100,
    bills: bills.map((b) => ({
      title: clean(b.title),
      amount: Number(b.amount) || 0,
      paidBy: clean(b.paidBy),
      date: clean(b.date),
    })),
    transfers: transfers.map((t) => ({
      from: clean(t.from),
      to: clean(t.to),
      amount: Number(t.amount) || 0,
    })),
  };

  try {
    const response = await axios.post(
        `${GEMINI_BASE}/models/${GEMINI_MODEL}:generateContent`,
        {
          systemInstruction: {parts: [{text: SETTLE_MESSAGE_INSTRUCTIONS}]},
          contents: [{role: "user", parts: [{text: JSON.stringify(payload)}]}],
          generationConfig: {temperature: 0.7},
        },
        {
          headers: {"x-goog-api-key": geminiKey, "Content-Type": "application/json"},
          timeout: 45000,
        }
    );

    const parts = response.data?.candidates?.[0]?.content?.parts || [];
    const message = parts.map((p: any) => p.text || "").join("").replace(/\*\*/g, "").trim();

    if (!message) {
      throw new Error("Gemini returned an empty message.");
    }

    return {success: true, message};
  } catch (error: any) {
    logger.error("Gemini settle message failed", {
      status: error?.response?.status,
      data: error?.response?.data,
      message: error?.message,
    });
    throw new HttpsError("internal", error?.response?.data?.error?.message || error?.message || "Message failed.");
  }
});