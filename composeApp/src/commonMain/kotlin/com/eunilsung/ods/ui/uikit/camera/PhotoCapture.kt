package com.eunilsung.ods.ui.uikit.camera

import androidx.compose.runtime.Composable

/**
 * 사진 한 장 찍기 — 돌려준 함수를 부르면 카메라가 열리고, 찍으면 JPEG 바이트를, 취소하거나 열 수 없으면 null 을 [onResult] 로 준다.
 *
 * - Android: 시스템 카메라 앱. 앱 캐시의 임시 파일로 원본을 받아 줄인 뒤 바로 지운다.
 * - iOS: UIImagePickerController. 카메라가 없는 기기(시뮬레이터)는 사진첩에서 고른다.
 *
 * 찍은 사진은 메모리에만 두고 저장하지 않는다(신분증 같은 민감 정보).
 */
@Composable
expect fun rememberPhotoCapture(onResult: (ByteArray?) -> Unit): () -> Unit
