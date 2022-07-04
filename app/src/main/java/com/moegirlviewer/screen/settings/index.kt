package com.moegirlviewer.screen.settings

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moegirlviewer.BuildConfig
import com.moegirlviewer.Constants
import com.moegirlviewer.R
import com.moegirlviewer.component.styled.StyledText
import com.moegirlviewer.component.styled.StyledTopAppBar
import com.moegirlviewer.screen.settings.component.SettingsScreenItem
import com.moegirlviewer.store.AccountStore
import com.moegirlviewer.store.CommonSettings
import com.moegirlviewer.store.SettingsStore
import com.moegirlviewer.screen.splashSetting.SplashImageMode
import com.moegirlviewer.theme.text
import com.moegirlviewer.util.*
import kotlinx.coroutines.launch

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun SettingsScreen() {
  val model: SettingsScreenModel = hiltViewModel()
  val scope = rememberCoroutineScope()
  val commonSettings by SettingsStore.common.getValue { this }.collectAsState(initial = CommonSettings())
  val isLoggedIn by AccountStore.isLoggedIn.collectAsState(initial = false)
  val themeColors = MaterialTheme.colors

  val switchColors = SwitchDefaults.colors(
    checkedThumbColor = themeColors.primaryVariant,
    uncheckedTrackAlpha = 0.54f,
    uncheckedTrackColor = themeColors.text.tertiary
  )

  fun setSettingItem(setter: CommonSettings.() -> Unit) {
    scope.launch {
      SettingsStore.common.setValue(setter)
    }
  }

  Scaffold(
    topBar = {
      StyledTopAppBar(
        title = {
          StyledText(
            text = stringResource(id = R.string.settings),
            color = themeColors.onPrimary
          )
        }
      )
    }
  ) {
    Column(
      modifier = Modifier
        .verticalScroll(rememberScrollState())
    ) {
      Title(R.string.article)
      SettingsScreenItem(
        title = stringResource(id = R.string.heimuSwitch),
        subtext = stringResource(id = R.string.heimuSwitchHelpText),
        onClick = {
          setSettingItem { this.heimu = !this.heimu }
        }
      ) {
        Switch(
          checked = commonSettings.heimu,
          colors = switchColors,
          onCheckedChange = {
            setSettingItem { this.heimu = it }
          }
        )
      }
      SettingsScreenItem(
        title = stringResource(id = R.string.stopAudioOnLeave),
        subtext = stringResource(id = R.string.stopAudioOnLeaveHelpText),
        onClick = {
          setSettingItem { this.stopMediaOnLeave = !this.stopMediaOnLeave }
        }
      ) {
        Switch(
          checked = commonSettings.stopMediaOnLeave,
          colors = switchColors,
          onCheckedChange = {
            setSettingItem { this.stopMediaOnLeave = it }
          }
        )
      }

      if (isMoegirl()) {
        SettingsScreenItem(
          title = stringResource(id = R.string.useSpecialCharSupportedFontInArticle),
          subtext = stringResource(id = R.string.useSpecialCharSupportedFontInArticleHelpText),
          onClick = {
            setSettingItem { this.useSpecialCharSupportedFontInArticle = !this.useSpecialCharSupportedFontInArticle }
          }
        ) {
          Switch(
            checked = commonSettings.useSpecialCharSupportedFontInArticle,
            colors = switchColors,
            onCheckedChange = {
              setSettingItem { this.useSpecialCharSupportedFontInArticle = it }
            }
          )
        }
      }

      SettingsScreenItem(
        title = stringResource(id = R.string.useSerifFont),
        subtext = stringResource(id = R.string.useSerifFontHelpText),
        onClick = {
          setSettingItem { this.useSerifFontInArticle = !this.useSerifFontInArticle }
        }
      ) {
        Switch(
          checked = commonSettings.useSerifFontInArticle,
          colors = switchColors,
          onCheckedChange = {
            setSettingItem { this.useSerifFontInArticle = it }
          }
        )
      }

      SettingsScreenItem(
        title = stringResource(id = R.string.focusMode),
        subtext = stringResource(id = R.string.focusModeHelpText),
        onClick = {
          setSettingItem { this.focusMode = !this.focusMode }
        }
      ) {
        Switch(
          checked = commonSettings.focusMode,
          colors = switchColors,
          onCheckedChange = {
            setSettingItem { this.focusMode = it }
          }
        )
      }

      SettingsScreenItem(
        title = stringResource(id = R.string.hideTopTemplates),
        subtext = stringResource(id = R.string.hideTopTemplatesHint),
        onClick = {
          setSettingItem { this.hideTopTemplates = !this.hideTopTemplates }
        }
      ) {
        Switch(
          checked = commonSettings.hideTopTemplates,
          colors = switchColors,
          onCheckedChange = {
            setSettingItem { this.hideTopTemplates = it }
          }
        )
      }

      Title(R.string._interface)
      if (isMoegirl()) {
        SettingsScreenItem(
          title = stringResource(id = R.string.useSpecialCharSupportedFontInApp),
          subtext = stringResource(id = R.string.useSpecialCharSupportedFontInAppHelpText),
          onClick = {
            setSettingItem { this.useSpecialCharSupportedFontInApp = !this.useSpecialCharSupportedFontInApp }
          }
        ) {
          Switch(
            checked = commonSettings.useSpecialCharSupportedFontInApp,
            colors = switchColors,
            onCheckedChange = {
              setSettingItem { this.useSpecialCharSupportedFontInApp = it }
            }
          )
        }
      }

      if (isMoegirl()) {
        SettingsScreenItem(
          title = stringResource(id = R.string.selectSplashScreenImage),
          onClick = {
            Globals.navController.navigate("splashSetting")
          }
        )
      } else {
        SettingsScreenItem(
          title = stringResource(id = R.string.showSplashScreen),
          innerVerticalPadding = false,
          onClick = {
            setSettingItem {
              // H萌娘只用NEW代表开启启动屏，OFF代表关闭，其他值不使用
              this.splashImageMode = if (this.splashImageMode == SplashImageMode.NEW)
                SplashImageMode.OFF else SplashImageMode.NEW
            }
          }
        ) {
          Switch(
            checked = commonSettings.splashImageMode == SplashImageMode.NEW,
            colors = switchColors,
            onCheckedChange = {
              setSettingItem {
                this.splashImageMode = if (it) SplashImageMode.OFF else SplashImageMode.NEW
              }
            }
          )
        }
      }

      SettingsScreenItem(
        title = stringResource(id = R.string.usePureTheme),
        innerVerticalPadding = false,
        onClick = {
          setSettingItem { this.usePureTheme = !this.usePureTheme }
        }
      ) {
        Switch(
          checked = commonSettings.usePureTheme,
          colors = switchColors,
          onCheckedChange = {
            setSettingItem { this.usePureTheme = it }
          }
        )
      }

//      SettingsScreenItem(
//        title = stringResource(id = R.string.darkThemeBySystem),
//        subtext = stringResource(id = R.string.darkThemeBySystemHelpText),
//        onClick = {
//          setSettingItem { this.darkThemeBySystem = !this.darkThemeBySystem }
//        }
//      ) {
//        Switch(
//          checked = commonSettings.darkThemeBySystem,
//          colors = switchColors,
//          onCheckedChange = {
//            setSettingItem { this.darkThemeBySystem = it }
//          }
//        )
//      }

//      Title(R.string.edit)
//      SettingsScreenItem(
//        title = stringResource(id = R.string.syntaxHighlight),
//        subtext = stringResource(id = R.string.syntaxHighlightHelpText),
//        onClick = {
//          setSettingItem { this.syntaxHighlight = !this.syntaxHighlight }
//        }
//      ) {
//        Switch(
//          checked = commonSettings.syntaxHighlight,
//          colors = switchColors,
//          onCheckedChange = {
//            setSettingItem { this.syntaxHighlight = it }
//          }
//        )
//      }

      Title(R.string.account)
      SettingsScreenItem(
        title = stringResource(
          if (isLoggedIn) R.string.logout else R.string.login
        ),
        onClick = {
          scope.launch { model.toggleLoginStatus() }
        }
      )

      Title(R.string.other)
//      SettingsScreenItem(
//        title = stringResource(id = R.string.lightRequestMode),
//        subtext = stringResource(id = R.string.lightRequestModeHelpText),
//        onClick = {
//          setSettingItem { this.lightRequestMode = !this.lightRequestMode }
//        }
//      ) {
//        Switch(
//          checked = commonSettings.lightRequestMode,
//          colors = switchColors,
//          onCheckedChange = {
//            setSettingItem { this.lightRequestMode = it }
//          }
//        )
//      }
      fun toggleStraightConnectMode() {
        setSettingItem { this.straightConnectMode = !this.straightConnectMode }
        needExitProcessOnExitApp = true
        toast(Globals.context.getString(R.string.becomeEffectiveInRestartAPP))
      }
      SettingsScreenItem(
        title = stringResource(id = R.string.straightConnectMode),
        subtext = stringResource(id = R.string.straightConnectModeHelpText),
        onClick = {
          toggleStraightConnectMode()
        }
      ) {
        Switch(
          checked = commonSettings.straightConnectMode,
          colors = switchColors,
          onCheckedChange = {
            toggleStraightConnectMode()
          }
        )
      }
      SettingsScreenItem(
        title = stringResource(id = R.string.privacyPolicy),
        onClick = {
          gotoArticlePage(Constants.privacyPageName)
        }
      )
      SettingsScreenItem(
        title = stringResource(id = R.string.disclaimer),
        onClick = {
          gotoArticlePage(Constants.disclaimerPageName)
        }
      )
      if (BuildConfig.FLAVOR_targetStore == "common") {
        SettingsScreenItem(
          title = stringResource(id = R.string.checkNewVersion),
          onClick = {
            scope.launch { model.checkNewVersion() }
          }
        )
      }

      Title(R.string.about)
      SettingsScreenItem(
        title = stringResource(id = R.string.developer),
        onClick = {
          gotoUserPage("東東君")
        }
      ) {
        StyledText(
          text = "User:東東君",
          fontSize = 14.sp,
          color = themeColors.primaryVariant,
          textDecoration = TextDecoration.Underline
        )
      }
    }
  }
}

@Composable
private fun Title(stringResourceId: Int) {
  val themeColors = MaterialTheme.colors

  StyledText(
    modifier = Modifier
      .padding(top = 10.dp, start = 10.dp, bottom = 5.dp),
    text = stringResource(id = stringResourceId),
    fontSize = 16.sp,
    color = themeColors.primaryVariant
  )
}