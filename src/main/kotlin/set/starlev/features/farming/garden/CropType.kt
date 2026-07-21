package set.starlev.features.farming.garden

import net.minecraft.world.item.Items

enum class CropType(
    val displayName: String,
    val toolPrefix: String,
    val iconItem: net.minecraft.world.item.Item,
) {
    WHEAT("Пшеница", "THEORETICAL_HOE_WHEAT", Items.WHEAT),
    CARROT("Морковь", "THEORETICAL_HOE_CARROT", Items.CARROT),
    POTATO("Картошка", "THEORETICAL_HOE_POTATO", Items.POTATO),
    NETHER_WART("Нарост", "THEORETICAL_HOE_WARTS", Items.NETHER_WART),
    PUMPKIN("Тыква", "PUMPKIN_DICER", Items.CARVED_PUMPKIN),
    MELON("Арбуз", "MELON_DICER", Items.MELON_SLICE),
    COCOA_BEANS("Какао-бобы", "COCO_CHOPPER", Items.COCOA_BEANS),
    SUGAR_CANE("Сахарный тростник", "THEORETICAL_HOE_CANE", Items.SUGAR_CANE),
    CACTUS("Кактус", "CACTUS_KNIFE", Items.CACTUS),
    MUSHROOM("Грибы", "THEORETICAL_HOE_MUSHROOM", Items.RED_MUSHROOM),
    ;
}
