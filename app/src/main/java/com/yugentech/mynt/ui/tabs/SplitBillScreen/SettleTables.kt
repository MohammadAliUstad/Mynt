package com.yugentech.mynt.ui.split.parent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yugentech.mynt.split.model.SplitTransfer
import com.yugentech.mynt.ui.shared.components.MyntCard
import com.yugentech.mynt.ui.shared.util.FinanceColors
import com.yugentech.mynt.ui.shared.util.LocalCurrencySymbol
import com.yugentech.mynt.ui.shared.util.formatAmount

@Composable
internal fun TableLabels() {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
        Text("Who owes", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = muted)
        Text("Amount", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = muted)
        Text("Pays to", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = muted)
    }
}

@Composable
internal fun TransferRow(from: String, to: String, amount: Double) {
    val currency = LocalCurrencySymbol.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            from,
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = FinanceColors.expense,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            formatAmount(amount, currency),
            Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Start,
            maxLines = 1
        )
        Text(
            to,
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = FinanceColors.income,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun SettleUpCard(transfers: List<SplitTransfer>, onSettle: () -> Unit) {
    MyntCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = 14.dp
    ) {
        Text(
            "Total settle-up",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        if (transfers.isEmpty()) {
            Text(
                "Everyone is settled up.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            TableLabels()
            transfers.forEachIndexed { index, transfer ->
                key(transfer.from, transfer.to) {
                    Column {
                        if (index > 0) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                        TransferRow(from = transfer.from, to = transfer.to, amount = transfer.amount)
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Button(
                onClick = onSettle,
                colors = ButtonDefaults.buttonColors(
                    containerColor = FinanceColors.borrowed,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Settled")
            }
        }
    }
}