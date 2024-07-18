package tornaco.apps.shortx.ext.ui

import android.content.Context
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tornaco.apps.shortx.core.I18N
import tornaco.apps.shortx.ui.base.ContextViewModel
import tornaco.apps.shortx.ui.main.model.DirectActionUM
import tornaco.apps.shortx.ui.main.model.daToDirectActionUM
import javax.inject.Inject


data class DAListState(val daList: List<DirectActionUM>)

@HiltViewModel
class DAListViewModel @Inject constructor(@ApplicationContext context: Context) :
    ContextViewModel<DAListState, Unit>(context, initState = { DAListState(emptyList()) }) {

    fun init() {
        logger.d("Init ${shortXManager.version()}")
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val daList = shortXManager.getAllDirectAction().map {
                    daToDirectActionUM(it, emptyList(), emptyList())
                }
                logger.d("daList: $daList")
                updateState { copy(daList = daList) }
            }
        }
    }

    override val i18N: I18N
        get() = object : I18N {
            override fun get(key: String, args: Map<String, String>, fallback: String?): String {
                return key
            }
        }
}