package com.yugentech.mynt.ui.split.parent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yugentech.mynt.ui.shared.util.LocalCurrencySymbol

@Composable
internal fun AmountInput(
    value: String,
    onValueChange: (String) -> Unit,
    color: Color,
    focusRequester: FocusRequester
) {
    val textStyle = MaterialTheme.typography.displayMedium.copy(
        fontWeight = FontWeight.Bold,
        color = color,
        // Removed TextAlign.Center -> The Row now centers the group perfectly without clipping!
        fontSize = if (value.length > 8) 40.sp else 52.sp
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = LocalCurrencySymbol.current,
            style = textStyle.copy(fontSize = 32.sp, color = color.copy(alpha = 0.7f))
        )
        Spacer(Modifier.width(6.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = textStyle,
            singleLine = true,
            cursorBrush = SolidColor(color),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            modifier = Modifier
                .width(IntrinsicSize.Min)
                .widthIn(min = 40.dp) // Keeps a small tap target when empty
                .focusRequester(focusRequester),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text("0", style = textStyle.copy(color = color.copy(alpha = 0.35f)))
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
internal fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
internal fun PillTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    capitalization: KeyboardCapitalization,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onDone: (() -> Unit)? = null
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        leadingIcon = leading,
        trailingIcon = trailing,
        singleLine = true,
        shape = RoundedCornerShape(20.dp),
        keyboardOptions = KeyboardOptions(capitalization = capitalization, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
internal fun NumberPillField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    TextField(
        value = value,
        onValueChange = { text ->
            if (text.matches(Regex("\\d*\\.?\\d{0,2}"))) onValueChange(text)
        },
        placeholder = { Text(placeholder) },
        singleLine = true,
        shape = RoundedCornerShape(20.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        modifier = Modifier.fillMaxWidth()
    )
}