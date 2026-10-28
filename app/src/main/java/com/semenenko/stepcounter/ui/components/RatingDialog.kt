package com.semenenko.stepcounter.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.semenenko.stepcounter.R

// Тёплый жёлтый цвет выбранных звёзд
private val StarColor = Color(0xFFFFB300)

/**
 * Окно «Оцените приложение»: пять звёзд, пояснение, куда пойдёт оценка, и кнопки «Отправить» / «Позже».
 * [onSubmit] получает число звёзд (1–5); [onLater] — пользователь закрыл окно, не оценив.
 */
@Composable
fun RatingDialog(onSubmit: (Int) -> Unit, onLater: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    var stars by rememberSaveable { mutableIntStateOf(0) } // сколько звёзд выбрано (0 — ещё не выбрано)

    AlertDialog(
        onDismissRequest = onLater,
        title = { Text(stringResource(R.string.rate_title)) },
        text = {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    stringResource(R.string.rate_text),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
                // Пять звёзд: нажатие на N-ю звезду выбирает оценку N
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    for (i in 1..5) {
                        Icon(
                            imageVector = if (i <= stars) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                            contentDescription = stringResource(R.string.rate_star_desc, i),
                            tint = if (i <= stars) StarColor else cs.outline,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable(role = Role.RadioButton) { stars = i }
                                .padding(4.dp),
                        )
                    }
                }
                // Подсказка, куда пойдёт оценка
                if (stars > 0) {
                    Text(
                        if (stars >= 4) stringResource(R.string.rate_hint_store) else stringResource(R.string.rate_hint_mail),
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(stars) }, enabled = stars > 0) { Text(stringResource(R.string.rate_submit)) }
        },
        dismissButton = { TextButton(onClick = onLater) { Text(stringResource(R.string.rate_later)) } },
    )
}
