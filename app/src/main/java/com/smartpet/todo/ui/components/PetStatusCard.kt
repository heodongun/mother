package com.smartpet.todo.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartpet.todo.data.PetMood
import com.smartpet.todo.data.PetState
import com.smartpet.todo.ui.TossColors

/**
 * Pet status card showing current mood and message
 */
@Composable
fun PetStatusCard(
    petState: PetState,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = when (petState.mood) {
            PetMood.HAPPY -> TossColors.PetHappy.copy(alpha = 0.1f)
            PetMood.WORRIED -> TossColors.PetWorried.copy(alpha = 0.1f)
            PetMood.CHASING -> TossColors.PetChasing.copy(alpha = 0.1f)
            PetMood.ANGRY -> TossColors.PetAngry.copy(alpha = 0.1f)
        },
        label = "bgColor"
    )
    
    val accentColor by animateColorAsState(
        targetValue = when (petState.mood) {
            PetMood.HAPPY -> TossColors.PetHappy
            PetMood.WORRIED -> TossColors.PetWorried
            PetMood.CHASING -> TossColors.PetChasing
            PetMood.ANGRY -> TossColors.PetAngry
        },
        label = "accentColor"
    )
    
    // Subtle bounce animation for urgent states
    val scale by animateFloatAsState(
        targetValue = if (petState.mood == PetMood.ANGRY || petState.mood == PetMood.CHASING) 1.02f else 1f,
        animationSpec = spring(dampingRatio = 0.3f),
        label = "scale"
    )
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = accentColor.copy(alpha = 0.3f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pet emoji with background circle
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = accentColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(28.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = petState.emoji,
                    fontSize = 28.sp
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "스마트펫",
                    fontSize = 12.sp,
                    color = TossColors.Gray500,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = petState.message,
                    fontSize = 16.sp,
                    color = TossColors.Black,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 22.sp
                )
            }
        }
    }
}
