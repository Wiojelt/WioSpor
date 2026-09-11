package wiospor

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class WioSporPlugin : Plugin() {
    private var mainApi: WioSpor? = null

    override fun load(context: Context) {
        val aggregator = SourceAggregator(context)
        val api = WioSpor(aggregator)
        mainApi = api
        registerMainAPI(api)

        openSettings = { uiContext ->
            WioSettings.show(uiContext, aggregator)
        }
    }
}
