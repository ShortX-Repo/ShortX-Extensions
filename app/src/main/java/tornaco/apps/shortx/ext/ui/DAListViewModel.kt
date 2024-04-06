package tornaco.apps.shortx.ext.ui

import android.content.Context
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tornaco.apps.shortx.core.proto.da.DirectAction
import tornaco.apps.shortx.core.shortXManager
import tornaco.apps.shortx.core.util.Logger
import tornaco.apps.shortx.ui.base.StateViewModel
import javax.inject.Inject


data class DAListState(val daList: List<DirectAction>)

@HiltViewModel
class DAListViewModel @Inject constructor(@ApplicationContext context: Context) :
    StateViewModel<DAListState>(initState = { DAListState(emptyList()) }) {
        private val logger = Logger("DAListViewModel")

    fun init() {
        logger.d("Init ${shortXManager.version()}")
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val daList = shortXManager.getAllDirectAction()
                logger.d("daList: $daList")
                updateState { copy(daList = daList) }
            }
        }
    }
}