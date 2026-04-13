package moe.nea.firmament.features

import moe.nea.firmament.events.FirmamentEvent
import moe.nea.firmament.events.subscription.Subscription
import moe.nea.firmament.events.subscription.SubscriptionList
import moe.nea.firmament.util.ErrorUtil
import moe.nea.firmament.util.compatloader.ICompatMeta

object FeatureManager {

	fun subscribeEvents() {
		SubscriptionList.allLists.forEach { list ->
			if (ICompatMeta.shouldLoad(list.javaClass.name))
				ErrorUtil.catch("Error while loading events from $list") {
					list.provideSubscriptions {
						subscribeSingleEvent(it)
					}
				}
		}
	}

	private fun <T : FirmamentEvent> subscribeSingleEvent(it: Subscription<T>) {
		it.eventBus.subscribe(false, "${it.owner.javaClass.simpleName}:${it.methodName}", it.invoke)
	}
}
