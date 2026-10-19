package com.eunilsung.ods.ui.uikit.pin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eunilsung.ods.domain.util.OrderPinPolicy
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.uikit.click.clickable

/**
 * 보안 키패드 — 입력 표시(●●●●)와 숫자 키. 국내 금융 앱처럼 **숫자 배열을 열 때마다 섞어** 누른 위치로 비밀번호를 짐작하지 못하게 한다.
 *
 * 4자리를 다 넣으면 [onEntered] 를 부르고 입력을 비운다. [resetKey] 가 바뀌면(틀렸을 때) 입력을 비우고 배열도 다시 섞는다.
 *
 * @param deleteLabel 지우기 키를 화면 읽기 프로그램이 읽을 이름.
 */
@Composable
fun SecurePinPad(
    onEntered: (String) -> Unit,
    resetKey: Int,
    deleteLabel: String,
    modifier: Modifier = Modifier,
) {
    var input by remember { mutableStateOf("") }
    val digits = remember(resetKey) { (0..9).map(Int::toString).shuffled() }
    LaunchedEffect(resetKey) { input = "" }

    fun press(digit: String) {
        if (input.length >= OrderPinPolicy.LENGTH) return
        input += digit
        if (input.length == OrderPinPolicy.LENGTH) {
            val entered = input
            input = ""
            onEntered(entered)
        }
    }

    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            repeat(OrderPinPolicy.LENGTH) { index ->
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(if (index < input.length) AppColors.PrimaryMain else AppColors.TextDisabled.copy(alpha = 0.4f)),
                )
            }
        }
        Spacer(Modifier.height(36.dp))
        for (row in 0..2) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                for (column in 0..2) {
                    val digit = digits[row * 3 + column]
                    Key(text = digit, onClick = { press(digit) })
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(KEY_SIZE))
            Key(text = digits[9], onClick = { press(digits[9]) })
            Box(
                modifier = Modifier
                    .size(KEY_SIZE)
                    .clip(CircleShape)
                    .clickable(onClick = { if (input.isNotEmpty()) input = input.dropLast(1) })
                    .semantics { contentDescription = deleteLabel },
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "⌫", color = AppColors.Text, fontSize = 24.sp)
            }
        }
    }
}

private val KEY_SIZE = 68.dp

@Composable
private fun Key(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(KEY_SIZE).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = AppColors.Text, fontSize = 26.sp, fontWeight = FontWeight.Medium)
    }
}
