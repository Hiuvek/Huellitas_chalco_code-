package com.example.huellitaschalco.nucleo.sistema_diseno

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.huellitaschalco.ui.theme.*
import com.example.huellitaschalco.nucleo.navegacion.*

@Composable
fun HInput(
    label: String,
    initial: String = "",
    placeholder: String = "",
    helper: String? = null,
    error: String? = null,
    password: Boolean = false,
    lines: Int = 1,
    keyboard: KeyboardType = KeyboardType.Text,
) {
    var text by rememberSaveable(label) { mutableStateOf(initial) }
    HInput(
        label = label,
        value = text,
        onValueChange = { text = it },
        placeholder = placeholder,
        helper = helper,
        error = error,
        password = password,
        lines = lines,
        keyboard = keyboard,
    )
}

/** Campo controlado: los formularios reales conservan los datos y errores en su ViewModel. */
@Composable
fun HInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    helper: String? = null,
    error: String? = null,
    password: Boolean = false,
    lines: Int = 1,
    keyboard: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true,
) {
    var showPassword by rememberSaveable(label) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier.fillMaxWidth(),
            enabled = enabled,
            placeholder = {
                Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = Muted)
            },
            textStyle = MaterialTheme.typography.bodyMedium,
            singleLine = lines == 1,
            minLines = lines,
            shape = RoundedCornerShape(13.dp),
            isError = error != null,
            keyboardOptions =
                KeyboardOptions(keyboardType = if (password) KeyboardType.Password else keyboard),
            visualTransformation =
                if (password && !showPassword) PasswordVisualTransformation()
                else VisualTransformation.None,
            trailingIcon =
                if (password)
                    ({
                        TextButton(onClick = { showPassword = !showPassword }) {
                            Text(
                                if (showPassword) "Ocultar" else "Mostrar",
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    })
                else null,
            colors =
                OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White,
                    errorContainerColor = Color.White,
                    unfocusedBorderColor = Line,
                    focusedBorderColor = Forest,
                ),
        )
        if (error != null)
            Text(error, color = Terracotta, style = MaterialTheme.typography.bodySmall)
        else if (helper != null) HCaption(helper)
    }
}

@Composable
fun HChoices(
    label: String?,
    options: List<String>,
    initial: String = options.first(),
    onChange: (String) -> Unit = {},
) {
    var selected by rememberSaveable(label) { mutableStateOf(initial) }
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        if (label != null) Text(label, style = MaterialTheme.typography.titleSmall)
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { option ->
                val active = option == selected
                Surface(
                    onClick = {
                        selected = option
                        onChange(option)
                    },
                    color = if (active) Forest else Color.White,
                    contentColor = if (active) Color.White else Ink,
                    shape = RoundedCornerShape(13.dp),
                    border = BorderStroke(1.dp, if (active) Forest else Line),
                ) {
                    Row(
                        Modifier.heightIn(min = 48.dp)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (active) Iconos(SimboloHuellitas.Check, Modifier.size(16.dp), Color.White)
                        Text(option, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun HCheck(text: String, initiallyChecked: Boolean = false) {
    var checked by rememberSaveable(text) { mutableStateOf(initiallyChecked) }
    HCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked, { checked = it })
            Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        }
    }
}
