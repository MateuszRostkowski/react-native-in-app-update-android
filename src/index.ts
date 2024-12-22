import { NativeModules, Platform } from 'react-native'

const { InAppUpdates } = NativeModules

export type AndroidCheckUpdateResult = 'SUCCESS' | 'IGNORED'

/**
 * Function for checking if there is an update available directly in PlayStore
 * @returns boolean
 */
export const checkPlayStoreUpdates = async () => {
  try {
    if (Platform.OS === 'ios') {
      console.log('checkPlayStoreUpdates is only available on Android')
      return
    }

    if (!InAppUpdates) {
      throw new Error('InAppUpdates object is not defined')
    }
    const isUpdateAvailable = await InAppUpdates?.isUpdateAvailable()
    return isUpdateAvailable as boolean
  } catch (e) {
    console.log('Error in checkPlayStoreUpdates', e)

    return false
  }
}

/**
 * Function fully handling in-app update for **Android**. Uses InAppUpdates native module.
 *
 * *checkUpdate()* function returns promise that has 3 possible results:
 *
 * 1) 'SUCCESS' - we just let the library handle update
 *
 * 2) 'IGNORED' - this will set 24h timer before showing next ANY update prompt
 *
 * 3) 'ERROR' - we let the library handle it
 */
export const performPlayStoreUpdate = async (latestUpdate: string, onIgnore?: (latestUpdate: string) => void) => {
  try {
    if (!InAppUpdates) {
      throw new Error('UPDATES: InAppUpdates object is not defined')
    }
    // result has 2 states: SUCCESS | IGNORED
    const result = await InAppUpdates?.checkUpdate()

    if (onIgnore && (result as AndroidCheckUpdateResult) === 'IGNORED') {
      onIgnore(latestUpdate)
    }
  } catch (e) {
    console.log('UPDATES: Error occured while checking Play Store updates', e)
  }
}