/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amplifyframework.ui.liveness.state

import android.graphics.RectF
import com.amplifyframework.ui.liveness.ml.FaceDetector
import com.amplifyframework.ui.liveness.ml.FaceDetector.Landmark
import io.mockk.InvokeMatcher
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import org.junit.Before
import org.junit.Test

internal class FaceDetectorTest {
    lateinit var detector: FaceDetector
    @Before
    fun setup() {
        val state = mockk<LivenessState>() {
            every { faceTargetChallenge } returns null
        }
        detector = FaceDetector(state)
    }

    @Test
    fun `test face detection algorithm`() {
        // given
        val faceBottom = 0.88746625f
        val leftEye = Landmark(0.668633f, 0.48738188f)
        val rightEye = Landmark(0.35714725f, 0.46644497f)
        val nose = Landmark(0.52836484f, 0.53194016f)
        val mouth = Landmark(0.5062596f, 0.68926525f)
        val leftEar = Landmark(0.78989476f, 0.5973732f)
        val rightEar = Landmark(0.16585289f, 0.5668279f)

        var rectLeft = 0f
        var rectTop = 0f
        var rectRight = 0f
        var rectBottom = 0f

        // RectF is stubbed in unit tests, so we need to access what it was called with in a different way
        mockkConstructor(RectF::class)
        every {
            constructedWith<RectF>(
                InvokeMatcher<Float> { rectLeft = it },
                InvokeMatcher<Float> { rectTop = it },
                InvokeMatcher<Float> { rectRight = it },
                InvokeMatcher<Float> { rectBottom = it },
            ).isEmpty
        } answers {
            assertClose(rectLeft, 0.16585289f)
            assertClose(rectTop, 0.04175697f)
            assertClose(rectRight, 0.16585289f + 0.62404186f)
            assertClose(rectBottom, 0.04175697f + 0.84570926f)
            true
        }

        detector.generateBoundingBoxFromLandmarks(
            faceBottom,
            leftEye,
            rightEye,
            nose,
            mouth,
            leftEar,
            rightEar
        ).isEmpty
    }

    private fun assertClose(actual: Float, expected: Float, difference: Float = 0.000001f) {
        assert(kotlin.math.abs(actual - expected) < difference)
    }
}