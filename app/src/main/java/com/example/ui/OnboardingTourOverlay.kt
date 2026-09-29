package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

data class TourStepData(
    val title: String,
    val description: String,
    val details: String,
    val icon: ImageVector,
    val iconColor: Color? = null
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingTourOverlay(
    step: Int,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onSkip: () -> Unit
) {
    val steps = listOf(
        TourStepData(
            title = "Bem-vindo(a) ao seu novo negócio! 🐶",
            description = "Sua conta novinha em folha foi criada com sucesso!",
            details = "Para garantir seu controle total, iniciamos seu banco de dados completamente limpo (sem cães ou clientes de teste).\n\nNo entanto, já deixamos pré-cadastrados os 3 procedimentos básicos do mercado (com valores padrão sugeridos) para você começar imediatamente sem complicação!",
            icon = Icons.Default.Pets
        ),
        TourStepData(
            title = "Agenda Diária 📅",
            description = "Controle o fluxo diário do seu pet shop.",
            details = "Aqui na aba Agenda, você gerencia todos os atendimentos do dia selecionado.\n\nVocê pode marcar como Pago, Finalizado ou Cancelado rapidamente. Quando o serviço for pago, a receita é gerada de forma automática!",
            icon = Icons.Default.CalendarToday
        ),
        TourStepData(
            title = "Clientes e Seus Pets 👥",
            description = "Gerencie quem realmente importa!",
            details = "Na aba Clientes, cadastre os tutores e seus respectivos pets (com raça, idade e notas).\n\nO grande diferencial: você pode definir observações de comportamento (alergias, medo do secador) e até valores de banho e transporte específicos para cada cliente!",
            icon = Icons.Default.People
        ),
        TourStepData(
            title = "Procedimentos e Valores 🏷️",
            description = "Ajuste os preços dos seus serviços.",
            details = "Na aba Preços, configure os valores que você cobra no seu estabelecimento.\n\nEdite o preço dos procedimentos padrão (Banho, Tosa, Tosa Higiênica) ou crie novos. Você também pode gerenciar o estoque e preço de venda de produtos!",
            icon = Icons.Default.Inventory2
        ),
        TourStepData(
            title = "Gestão Financeira 💰",
            description = "Fluxo de caixa sem complicação.",
            details = "Na aba Financeiro, controle a saúde do seu negócio.\n\nCada atendimento agendado e marcado como Pago lança uma receita automática aqui. Você também pode cadastrar despesas (shampoos, contas, luz) e ver seu lucro líquido real!",
            icon = Icons.Default.Paid
        ),
        TourStepData(
            title = "Tudo Pronto! 🚀",
            description = "Seu negócio organizado na palma da mão.",
            details = "Chega de perder tempo com anotações em papéis e planilhas confusas. Agora você tem o controle profissional dos atendimentos, clientes e do financeiro!\n\nCadastre seu primeiro cliente e marque seu primeiro banho!",
            icon = Icons.Default.CheckCircle,
            iconColor = Color(0xFF4CAF50)
        )
    )

    val currentData = steps.getOrNull(step) ?: return

    Dialog(
        onDismissRequest = { /* Force tour to be completed or skipped */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            ElevatedCard(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header with Skip option
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "PASSO ${step + 1} DE ${steps.size}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        if (step < steps.size - 1) {
                            Text(
                                text = "Pular tour",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { onSkip() }
                                    .padding(8.dp)
                                    .testTag("btn_skip_tour")
                            )
                        }
                    }

                    // Content animations
                    AnimatedContent(
                        targetState = currentData,
                        transitionSpec = {
                            fadeIn() with fadeOut()
                        },
                        label = "tour_step_content"
                    ) { stepData ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Icon Box
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = stepData.icon,
                                    contentDescription = null,
                                    tint = stepData.iconColor ?: MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            // Title
                            Text(
                                text = stepData.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )

                            // Description
                            Text(
                                text = stepData.description,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )

                            // Details block
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = stepData.details,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp),
                                    textAlign = TextAlign.Start,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }

                    // Progress indicator dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        steps.forEachIndexed { index, _ ->
                            Box(
                                modifier = Modifier
                                    .size(if (index == step) 12.dp else 8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (index == step) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant
                                    )
                            )
                        }
                    }

                    // Navigation buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (step > 0) {
                            OutlinedButton(
                                onClick = onPrev,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("btn_tour_prev")
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Voltar", fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = onNext,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("btn_tour_next")
                        ) {
                            Text(
                                text = if (step == steps.size - 1) "Começar! 🐾" else "Avançar",
                                fontWeight = FontWeight.ExtraBold
                            )
                            if (step < steps.size - 1) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
