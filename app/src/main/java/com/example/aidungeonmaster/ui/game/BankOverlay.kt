package com.example.aidungeonmaster.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.aidungeonmaster.viewmodel.InventoryViewModel
import kotlinx.coroutines.launch

@Composable
fun BankOverlay(
    bankName: String,
    gameId: String,
    currentCoins: Int,
    inventoryViewModel: InventoryViewModel,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(true) }
    var authenticated by remember { mutableStateOf(false) }
    var hasPin by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var bankBalance by remember { mutableIntStateOf(0) }
    var cashCoins by remember { mutableIntStateOf(currentCoins) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var processing by remember { mutableStateOf(false) }

    LaunchedEffect(gameId) {
        loading = true
        val account = inventoryViewModel.getBankAccount(gameId)
        hasPin = account != null
        bankBalance = (account?.get("balance") as? Number)?.toInt() ?: 0
        loading = false
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .border(2.dp, Color(0xFFFFD700), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xEE101820))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = Color(0xFFFFD700)
                    )
                    Text(
                        text = " Banco de $bankName",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                }
            }

            Spacer(Modifier.height(12.dp))

            if (loading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFFFD700))
                }
                return@Column
            }

            Text("🪙 Monedas en mano: $cashCoins", color = Color(0xFFFFD700))
            Text("🏦 Saldo en banco: $bankBalance", color = Color(0xFF90CAF9))
            Spacer(Modifier.height(12.dp))

            when {
                !hasPin -> {
                    Text(
                        "Primera visita: registra un PIN numérico de 4 dígitos.",
                        color = Color.White
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pin,
                        onValueChange = {
                            if (it.length <= 4 && it.all(Char::isDigit)) pin = it
                        },
                        label = { Text("PIN") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                processing = true
                                val ok = inventoryViewModel.registerBankPin(gameId, pin)
                                if (ok) {
                                    hasPin = true
                                    authenticated = true
                                    feedback = "✅ PIN registrado correctamente"
                                } else {
                                    feedback = "❌ El PIN debe tener 4 dígitos y no existir ya una cuenta"
                                }
                                processing = false
                            }
                        },
                        enabled = !processing && pin.length == 4,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Registrar PIN")
                    }
                }

                !authenticated -> {
                    Text("Introduce tu PIN para operar en el banco.", color = Color.White)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pin,
                        onValueChange = {
                            if (it.length <= 4 && it.all(Char::isDigit)) pin = it
                        },
                        label = { Text("PIN") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                processing = true
                                authenticated = inventoryViewModel.verifyBankPin(gameId, pin)
                                feedback = if (authenticated) {
                                    "✅ Acceso concedido"
                                } else {
                                    "❌ PIN incorrecto"
                                }
                                processing = false
                            }
                        },
                        enabled = !processing && pin.length == 4,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Acceder")
                    }
                }

                else -> {
                    Text(
                        "Deposita o retira monedas de tu reserva personal.",
                        color = Color.White
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = amount,
                        onValueChange = {
                            if (it.all(Char::isDigit)) amount = it
                        },
                        label = { Text("Cantidad") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                scope.launch {
                                    val value = amount.toIntOrNull() ?: 0
                                    processing = true
                                    val ok = inventoryViewModel.depositToBank(gameId, value)
                                    if (ok) {
                                        cashCoins -= value
                                        bankBalance += value
                                        amount = ""
                                        feedback = "✅ Has depositado $value monedas"
                                    } else {
                                        feedback = "❌ No tienes saldo suficiente o la operación es inválida"
                                    }
                                    processing = false
                                }
                            },
                            enabled = !processing && (amount.toIntOrNull() ?: 0) > 0,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Depositar")
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    val value = amount.toIntOrNull() ?: 0
                                    processing = true
                                    val ok = inventoryViewModel.withdrawFromBank(gameId, value)
                                    if (ok) {
                                        cashCoins += value
                                        bankBalance -= value
                                        amount = ""
                                        feedback = "✅ Has retirado $value monedas"
                                    } else {
                                        feedback = "❌ El banco no tiene saldo suficiente o la operación es inválida"
                                    }
                                    processing = false
                                }
                            },
                            enabled = !processing && (amount.toIntOrNull() ?: 0) > 0,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Retirar")
                        }
                    }
                }
            }

            feedback?.let {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = it,
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x33000000), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                )
            }
        }
    }
}