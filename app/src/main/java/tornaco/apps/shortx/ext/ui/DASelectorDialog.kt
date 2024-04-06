@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class)

package tornaco.apps.shortx.ext.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import tornaco.apps.shortx.core.proto.da.DirectAction
import tornaco.apps.shortx.core.res.Remix
import tornaco.apps.shortx.ui.base.CommonDialogState
import tornaco.apps.shortx.ui.base.ListItem
import tornaco.apps.shortx.ui.base.RemixIcon
import tornaco.apps.shortx.ui.base.ShortFullScreenXDialog
import tornaco.apps.shortx.ui.base.ShortXTopAppBar
import tornaco.apps.shortx.ui.theme.ShortXTheme


class DADialogState(
    val onSelected: (DirectAction) -> Unit
) : CommonDialogState()

@Composable
fun rememberDADialogState(
    onSelected: (DirectAction) -> Unit
): DADialogState {
    return remember {
        DADialogState(onSelected)
    }
}

@Composable
fun DASelectorDialog(state: DADialogState) {
    if (state.isShowing) {
        ShortFullScreenXDialog(onDismissRequest = { state.dismiss() }) {
            DASelector(state)
        }
    }
}

@Composable
private fun DASelector(dialogState: DADialogState) {
    val daListViewModel = hiltViewModel<DAListViewModel>().apply { resetState() }
    val daListState by daListViewModel.state.collectAsState()

    LaunchedEffect(daListViewModel) {
        daListViewModel.init()
    }

    ShortXTheme {
        Surface(
            modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(modifier = Modifier,
                topBar = {
                    ShortXTopAppBar(
                        modifier = Modifier,
                        title = { Text("Select Rule") },
                        navigationIcon = {
                            IconButton(onClick = {
                                dialogState.dismiss()
                            }) {
                                RemixIcon(remixName = Remix.System.close_line)
                            }
                        },
                        actions = {
                        },
                    )
                }) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentPadding = it
                ) {
                    items(daListState.daList) {
                        ListItem(
                            title = it.title,
                            onClick = {
                                dialogState.onSelected(it)
                            })
                    }
                }
            }
        }
    }

}