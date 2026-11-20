package vn.miaosong.core.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import vn.miaosong.core.designsystem.theme.MiaoColors

/**
 * Port 1:1 từ file `docs/ui_design/stitch_miaosong_delivery_app_ui/animated_svg/code.html`
 * (STITCH_SVG: ANIMATION_35 - Mèo thần tài Maneki-Neko đội mũ shipper tia sét, vẫy tay đón khách).
 */
@Composable
fun MiaoAnimatedCatLogo(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    showSquircleBackground: Boolean = true,
    animated: Boolean = true,
) {
    val armRotation: Float
    val armScale: Float
    val waveGlowAlpha: Float

    if (animated) {
        val transition = rememberInfiniteTransition(label = "miao_cat_wave")

        // @keyframes waveBackToFront (1.25s infinite)
        val rot by transition.animateFloat(
            initialValue = 0f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 1250
                    0f at 0 using FastOutSlowInEasing
                    (-14f) at 375 using FastOutSlowInEasing
                    4f at 687 using FastOutSlowInEasing
                    (-12f) at 937 using FastOutSlowInEasing
                    0f at 1250
                },
            ),
            label = "armRotation",
        )

        val sc by transition.animateFloat(
            initialValue = 1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 1250
                    1.0f at 0
                    1.06f at 375
                    0.96f at 687
                    1.04f at 937
                    1.0f at 1250
                },
            ),
            label = "armScale",
        )

        val glow by transition.animateFloat(
            initialValue = 0.25f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(625, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "waveGlowAlpha",
        )

        armRotation = rot
        armScale = sc
        waveGlowAlpha = glow
    } else {
        // Ảnh tĩnh (static) cho Header và Thẻ chào trên trang Home
        armRotation = 0f
        armScale = 1f
        waveGlowAlpha = 0.85f
    }

    Canvas(modifier = modifier.size(size)) {
        val scale = this.size.minDimension / 512f
        withTransform({
            scale(scale, scale, pivot = Offset.Zero)
        }) {
            drawMiaoCatVector(
                armRotation = armRotation,
                armScale = armScale,
                waveGlowAlpha = waveGlowAlpha,
                showSquircleBackground = showSquircleBackground,
            )
        }
    }
}

private fun DrawScope.drawMiaoCatVector(
    armRotation: Float,
    armScale: Float,
    waveGlowAlpha: Float,
    showSquircleBackground: Boolean,
) {
    val bgGrad = Brush.linearGradient(
        colors = listOf(Color(0xFFFF6B00), Color(0xFFFF3800)),
        start = Offset.Zero,
        end = Offset(512f, 512f),
    )
    val tealGrad = Brush.linearGradient(
        colors = listOf(Color(0xFF00E5C9), Color(0xFF00B49F)),
        start = Offset(100f, 160f),
        end = Offset(400f, 430f),
    )
    val furGrad = Brush.verticalGradient(
        colors = listOf(Color(0xFFFFFFFF), Color(0xFFF4F5FA)),
        startY = 100f,
        endY = 490f,
    )
    val armGrad = Brush.verticalGradient(
        colors = listOf(Color(0xFFFFFFFF), Color(0xFFF2F3F8), Color(0xFFE2E5F0)),
        startY = 230f,
        endY = 408f,
    )
    val pinkBean = Color(0xFFFFAA80)

    // 1. Background Squircle
    if (showSquircleBackground) {
        drawRoundRect(
            brush = bgGrad,
            topLeft = Offset.Zero,
            size = Size(512f, 512f),
            cornerRadius = CornerRadius(128f, 128f),
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.25f),
            topLeft = Offset(16f, 16f),
            size = Size(480f, 480f),
            cornerRadius = CornerRadius(112f, 112f),
            style = Stroke(width = 6f),
        )
    }

    // 2. LAYER A: BODY & CHEST
    val bodyPath = Path().apply {
        moveTo(148f, 358f)
        cubicTo(125f, 400f, 132f, 468f, 175f, 482f)
        cubicTo(215f, 492f, 295f, 492f, 335f, 482f)
        cubicTo(382f, 468f, 390f, 400f, 365f, 358f)
        close()
    }
    drawPath(bodyPath, brush = furGrad)

    // Bụng trắng
    drawOval(
        color = Color.White,
        topLeft = Offset(256f - 66f, 436f - 38f),
        size = Size(132f, 76f),
    )

    // Bàn chân trái ôm bụng
    withTransform({
        rotate(degrees = -15f, pivot = Offset(180f, 425f))
    }) {
        drawOval(
            color = Color.White,
            topLeft = Offset(180f - 22f, 425f - 16f),
            size = Size(44f, 32f),
        )
        drawOval(
            color = Color(0xFFFFE5D9),
            topLeft = Offset(180f - 22f, 425f - 16f),
            size = Size(44f, 32f),
            style = Stroke(width = 2f),
        )
    }
    drawCircle(color = pinkBean, radius = 3.5f, center = Offset(172f, 420f))
    drawCircle(color = pinkBean, radius = 3.5f, center = Offset(182f, 418f))
    drawCircle(color = pinkBean, radius = 3.5f, center = Offset(191f, 422f))

    // 3. LAYER B: CÁNH TAY VẪY TỪ SAU RA TRƯỚC (Pivot: 375, 365)
    withTransform({
        rotate(degrees = armRotation, pivot = Offset(375f, 365f))
        scale(scaleX = armScale, scaleY = armScale, pivot = Offset(375f, 365f))
    }) {
        val armPath = Path().apply {
            moveTo(340f, 378f)
            cubicTo(362f, 360f, 382f, 340f, 402f, 295f)
            cubicTo(418f, 258f, 438f, 232f, 458f, 245f)
            cubicTo(475f, 258f, 472f, 292f, 452f, 338f)
            cubicTo(432f, 382f, 405f, 408f, 360f, 398f)
            close()
        }
        drawPath(armPath, brush = armGrad)

        // Đệm lòng bàn chân mèo
        withTransform({
            rotate(degrees = -12f, pivot = Offset(442f, 272f))
        }) {
            drawOval(
                color = pinkBean,
                topLeft = Offset(442f - 16f, 272f - 13f),
                size = Size(32f, 26f),
            )
        }
        // Ngón đệm thịt hồng
        drawCircle(color = pinkBean, radius = 6f, center = Offset(423f, 250f))
        drawCircle(color = pinkBean, radius = 6.5f, center = Offset(439f, 242f))
        drawCircle(color = pinkBean, radius = 6f, center = Offset(456f, 248f))

        // Sóng chào đón năng lượng
        val wave1 = Path().apply {
            moveTo(474f, 232f)
            quadraticTo(488f, 248f, 482f, 270f)
        }
        val wave2 = Path().apply {
            moveTo(488f, 242f)
            quadraticTo(502f, 260f, 495f, 282f)
        }
        drawPath(
            path = wave1,
            color = Color.White.copy(alpha = waveGlowAlpha),
            style = Stroke(
                width = 4.5f,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f)),
            ),
        )
        drawPath(
            path = wave2,
            color = Color.White.copy(alpha = waveGlowAlpha * 0.8f),
            style = Stroke(width = 3.5f, cap = StrokeCap.Round),
        )
    }

    // 4. LAYER C: HEAD & FACE
    // Tai trái
    drawPath(
        Path().apply {
            moveTo(110f, 210f); lineTo(150f, 90f); lineTo(230f, 160f); close()
        },
        color = Color.White,
    )
    drawPath(
        Path().apply {
            moveTo(125f, 195f); lineTo(158f, 120f); lineTo(210f, 168f); close()
        },
        color = pinkBean,
    )
    // Tai phải
    drawPath(
        Path().apply {
            moveTo(380f, 210f); lineTo(340f, 90f); lineTo(260f, 160f); close()
        },
        color = Color.White,
    )
    drawPath(
        Path().apply {
            moveTo(365f, 195f); lineTo(332f, 120f); lineTo(280f, 168f); close()
        },
        color = pinkBean,
    )

    // Mũ Shipper Công Nghệ
    val capCrown = Path().apply {
        moveTo(120f, 185f)
        quadraticTo(245f, 115f, 370f, 185f)
        lineTo(380f, 225f)
        quadraticTo(245f, 170f, 110f, 225f)
        close()
    }
    drawPath(capCrown, color = Color(0xFF1C1B1F))

    val capVisor = Path().apply {
        moveTo(108f, 220f)
        quadraticTo(245f, 165f, 382f, 220f)
        quadraticTo(405f, 248f, 372f, 252f)
        quadraticTo(245f, 215f, 118f, 252f)
        quadraticTo(88f, 248f, 108f, 220f)
        close()
    }
    drawPath(capVisor, brush = tealGrad)

    // Tia sét vàng trên mũ
    val capBolt = Path().apply {
        moveTo(252f, 160f)
        lineTo(234f, 192f)
        lineTo(248f, 192f)
        lineTo(236f, 218f)
        lineTo(264f, 185f)
        lineTo(250f, 185f)
        close()
    }
    drawPath(capBolt, color = Color(0xFFFFE500))

    // Khuôn mặt mèo tròn bầu bĩnh
    drawOval(
        color = Color.White,
        topLeft = Offset(245f - 142f, 295f - 122f),
        size = Size(284f, 244f),
    )

    // Má hồng
    drawOval(
        color = Color(0xFFFF8A73).copy(alpha = 0.65f),
        topLeft = Offset(160f - 23f, 315f - 15f),
        size = Size(46f, 30f),
    )
    drawOval(
        color = Color(0xFFFF8A73).copy(alpha = 0.65f),
        topLeft = Offset(330f - 23f, 315f - 15f),
        size = Size(46f, 30f),
    )

    // Đôi mắt to tròn long lanh
    val eyeDark = Color(0xFF1F1F24)
    drawCircle(color = eyeDark, radius = 23f, center = Offset(188f, 265f))
    drawCircle(color = Color.White, radius = 8.5f, center = Offset(181f, 257f))
    drawCircle(color = Color.White, radius = 4.2f, center = Offset(196f, 273f))

    drawCircle(color = eyeDark, radius = 23f, center = Offset(302f, 265f))
    drawCircle(color = Color.White, radius = 8.5f, center = Offset(295f, 257f))
    drawCircle(color = Color.White, radius = 4.2f, center = Offset(310f, 273f))

    // Mũi tam giác cam & miệng chữ w
    drawPath(
        Path().apply {
            moveTo(245f, 288f); lineTo(237f, 279f); lineTo(253f, 279f); close()
        },
        color = Color(0xFFFF6B00),
    )
    val mouthPath = Path().apply {
        moveTo(227f, 295f)
        cubicTo(233f, 306f, 243f, 306f, 245f, 299f)
        cubicTo(247f, 306f, 257f, 306f, 263f, 295f)
    }
    drawPath(
        mouthPath,
        color = Color(0xFF2B2A2D),
        style = Stroke(width = 4.5f, cap = StrokeCap.Round),
    )

    // Râu mèo 2 bên má
    val whiskerColor = Color(0xFFFFD2C0)
    drawLine(whiskerColor, Offset(135f, 280f), Offset(85f, 270f), strokeWidth = 4f, cap = StrokeCap.Round)
    drawLine(whiskerColor, Offset(137f, 298f), Offset(90f, 304f), strokeWidth = 4f, cap = StrokeCap.Round)
    drawLine(whiskerColor, Offset(355f, 280f), Offset(405f, 272f), strokeWidth = 4f, cap = StrokeCap.Round)
    drawLine(whiskerColor, Offset(353f, 298f), Offset(400f, 304f), strokeWidth = 4f, cap = StrokeCap.Round)

    // 5. LAYER D: COLLAR & BELL
    val collarPath = Path().apply {
        moveTo(180f, 392f)
        quadraticTo(245f, 412f, 310f, 392f)
        lineTo(315f, 408f)
        quadraticTo(245f, 430f, 175f, 408f)
        close()
    }
    drawPath(collarPath, brush = tealGrad)
    drawCircle(color = Color(0xFFFFE500), radius = 18f, center = Offset(245f, 418f))
    drawCircle(
        color = Color.White,
        radius = 18f,
        center = Offset(245f, 418f),
        style = Stroke(width = 3f),
    )
    val bellBolt = Path().apply {
        moveTo(247f, 408f)
        lineTo(240f, 418f)
        lineTo(246f, 418f)
        lineTo(242f, 428f)
        lineTo(251f, 415f)
        lineTo(246f, 415f)
        close()
    }
    drawPath(bellBolt, color = Color(0xFFFF5A00))
}

/**
 * Màn hình Loading / Splash Overlay với nền đen mờ (scrim) và chú mèo thần tài MiaoSong cỡ lớn
 * đang vẫy tay (ANIMATION_35 từ Stitch).
 */
@Composable
fun MiaoLoadingOverlay(
    visible: Boolean,
    message: String = "MiaoSong đang gọi món ngon...",
    onDismiss: (() -> Unit)? = null,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(220)),
        exit = fadeOut(tween(280)),
    ) {
        val transition = rememberInfiniteTransition(label = "loading_pulse")
        val haloScale by transition.animateFloat(
            initialValue = 0.92f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "haloScale",
        )
        val dotPhase by transition.animateFloat(
            initialValue = 0f,
            targetValue = 3f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200),
                repeatMode = RepeatMode.Restart,
            ),
            label = "dotPhase",
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xD90E0906)) // Nền đen mờ ấm (85% opacity)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = { onDismiss?.invoke() },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    // Vầng sáng cam mờ phía sau mèo
                    Box(
                        modifier = Modifier
                            .size((248 * haloScale).dp)
                            .clip(RoundedCornerShape(64.dp))
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        MiaoColors.BrandOrange.copy(alpha = 0.42f),
                                        Color.Transparent,
                                    ),
                                ),
                            ),
                    )
                    // Con mèo thần tài vẫy tay cỡ lớn (210.dp)
                    MiaoAnimatedCatLogo(
                        size = 210.dp,
                        modifier = Modifier
                            .shadow(
                                elevation = 28.dp,
                                shape = RoundedCornerShape(54.dp),
                                ambientColor = MiaoColors.BrandOrange,
                                spotColor = MiaoColors.BrandOrange,
                            )
                            .clip(RoundedCornerShape(54.dp)),
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "MiaoSong 喵送",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFFFDBCD),
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 3 chấm nhảy theo nhịp vẫy tay của mèo
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(3) { idx ->
                        val active = dotPhase.toInt() % 3 == idx
                        Box(
                            modifier = Modifier
                                .size(if (active) 10.dp else 7.dp)
                                .clip(CircleShape)
                                .background(
                                    if (active) MiaoColors.BrandOrange else Color.White.copy(alpha = 0.35f),
                                ),
                        )
                    }
                }
            }
        }
    }
}
