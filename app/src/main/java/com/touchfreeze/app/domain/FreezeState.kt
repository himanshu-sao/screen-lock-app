package com.touchfreeze.app.domain

/**
 * Represents the touch freeze state machine.
 *
 * States:
 * - Idle: No video selected, no freeze active
 * - Playing: Video is playing with normal touch interaction
 * - Frozen: Video is playing but touch is blocked by overlay
 * - UnlockGestureDetected: Hidden gesture detected, transitioning to auth
 * - AuthChallenge: PIN entry bottom sheet is visible
 * - Unlocked: Auth successful, touch restored temporarily
 */
sealed class FreezeState {
    data object Idle : FreezeState()
    data object Playing : FreezeState()
    data object Frozen : FreezeState()
    data object UnlockGestureDetected : FreezeState()
    data object AuthChallenge : FreezeState()
    data object Unlocked : FreezeState()
}
