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

    logger.info("Sending PCM/WAV audio to Soniox STT API...", {
      pcmLength: rawPcmBuffer.length,
      wavLength: wavBuffer.length,
    });

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

    logger.info("Soniox transcription successful:", {textResult});

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

// ===================== Gemini: parse / update transactions =====================

const GEMINI_BASE = "https://generativelanguage.googleapis.com/v1beta";
const GEMINI_MODEL = "gemini-3.8-flash";

const EXPENSE_CATEGORIES = ["Food", "Travel", "Shopping", "Bills", "Entertainment", "EMI", "Other"];
const INCOME_CATEGORIES = ["Salary", "Freelance", "Gift", "Investment", "Other"];

interface CandidateTransaction {
  id: string;
  title: string;
  amount: number;
  category: string;
  date: string;
  type: string;
  personName?: string;
}

interface ParseTransactionsData {
  text?: string;
  today?: string;
  recentTransactions?: CandidateTransaction[];
}

const PARSER_INSTRUCTIONS = `You parse personal finance voice commands in English, Hindi (Devanagari), or Hinglish (Hindi written in English letters), including sentences that mix them.

LANGUAGE RULES:
- Understand numbers spoken as words in any of these languages. Examples: "इक्यावन हज़ार पाँच सौ", "ikyavan hazaar paanch sau" and "fifty one thousand five hundred" all mean 51500. "साढ़े बावन हज़ार" means 52500.
- Dates can be spoken in any language ("1st October", "1 अक्टूबर", "pehli October", "kal" = yesterday, "parso" = day before yesterday). Convert them to YYYY-MM-DD using "Today".
- When matching an existing transaction, compare meaning, not spelling. Common equivalents: salary = तनख्वाह / पगार / वेतन; rent = किराया / kiraya; food = खाना / khana; travel = सफर / yatra; bill = बिल; EMI = किस्त / kist; lent = उधार दिया / udhaar diya; borrowed = उधार लिया / udhaar liya.
- Always write output values in English: categories from the allowed lists, and for UPDATE keep the matched candidate's existing title exactly as it is.

CRITICAL UPDATE RULE:
If the user indicates they want to change, edit, correct, or update an existing transaction (for example: "change 1st October salary to 52,500", "make changes in the previous transaction", "50000 nahi 52500 tha", "badal do", "theek kar do", "salary 51500 kar do", "पिछली एंट्री को 300 कर दो", "किराये की जगह 15000 कर दो"):
1. Inspect the provided "Recent Transactions for matching" list. It is ordered newest first.
2. Find the candidate that best matches the description (match by title, category, date, or previous amount).
3. If several candidates match, prefer the one whose date matches what the user said. If the user says "previous", "last" or "pichhla", choose the first matching item in the list.
4. Set action = "UPDATE".
5. Set id = the EXACT "id" from that matching candidate in the list.
6. Set amount = the newly requested amount (e.g. 52500).
7. Keep the candidate's existing type, category, title, and date unless the user explicitly tells you to change those too.
8. NEVER set action = "CREATE" when the user is trying to change or correct an existing entry.

NEW TRANSACTIONS:
If the user is NOT modifying an existing entry:
- Set action = "CREATE".
- Set id = "".
- For LENT: Category "Lending". Set personName and title to the recipient's name if specified, otherwise title = "Lent".
- For BORROWED: Category "Borrowing". Set personName and title to the lender's name if specified, otherwise title = "Borrowed".
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
          action: {type: "STRING", enum: ["CREATE", "UPDATE"]},
          id: {type: "STRING"},
          type: {type: "STRING", enum: ["EXPENSE", "INCOME", "LENT", "BORROWED"]},
          title: {type: "STRING"},
          personName: {type: "STRING"},
          amount: {type: "NUMBER"},
          category: {type: "STRING"},
          date: {type: "STRING"},
        },
        required: ["action", "type", "title", "amount", "category", "date"],
      },
    },
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

  try {
    logger.info("Sending text to Gemini for parsing...", {spokenText, today, candidatesCount: candidateList.length});

    const response = await axios.post(
        `${GEMINI_BASE}/models/${GEMINI_MODEL}:generateContent`,
        {
          systemInstruction: {parts: [{text: PARSER_INSTRUCTIONS}]},
          contents: [{
            role: "user",
            parts: [{text: `Today: ${today}${candidateContext}\nSpoken command: "${spokenText}"`}],
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

    const entries = (Array.isArray(parsed.entries) ? parsed.entries : [])
        .map((e: any) => {
          const action = String(e.action).toUpperCase() === "UPDATE" ? "UPDATE" : "CREATE";
          const rawType = String(e.type).toUpperCase();
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

          return {
            action,
            id: String(e.id || "").trim(),
            type,
            title,
            personName,
            amount: Number(e.amount),
            category,
            date: dateRegex.test(String(e.date)) ? String(e.date) : today,
          };
        })
        .filter((e: any) => Number.isFinite(e.amount) && e.amount > 0);

    logger.info("Gemini parsing output:", {entries});

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