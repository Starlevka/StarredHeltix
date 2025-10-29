package set.starlev.starredheltix.util.qol;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.Style;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.util.Formatting;
import net.minecraft.client.sound.PositionedSoundInstance;
import set.starlev.starredheltix.sound.ModSounds;

import java.time.LocalDate;

public class VotingReminder {
    private static final String REMINDER_MESSAGE = "§6§lНе забудьте проголосовать за сервер! §e[Нажмите чтобы проголосовать]";
    private static final String TITLE_MESSAGE = "§6Голосование за сервер!";
    private static final String SUBTITLE_MESSAGE = "§eПомогите серверу стать лучше!";
    private static final String VOTE_COMMAND = "/голосование";
    private static final String THANK_YOU_MESSAGE = "§aСпасибо за голосование! Напоминание отключено до завтра.";


    private static boolean votingReminderEnabled = true;

    public static void checkAndShowReminder() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || !votingReminderEnabled) {
            return;
        }

        String today = LocalDate.now().toString();

        // Сброс на новый день
        if (!today.equals(set.starlev.starredheltix.client.StarredHeltixClient.CONFIG.lastVotingDate)) {
            set.starlev.starredheltix.client.StarredHeltixClient.CONFIG.lastVotingDate = today;
            set.starlev.starredheltix.client.StarredHeltixClient.CONFIG.hasVotedToday = false;
            set.starlev.starredheltix.client.StarredHeltixClient.CONFIG.hasShownVotingReminderToday = false;
            set.starlev.starredheltix.client.StarredHeltixClient.CONFIG.save();
        }

        // Показываем напоминание если еще не голосовали сегодня и еще не показывали сегодня
        if (!set.starlev.starredheltix.client.StarredHeltixClient.CONFIG.hasVotedToday && 
            !set.starlev.starredheltix.client.StarredHeltixClient.CONFIG.hasShownVotingReminderToday) {
            showReminder(client);
            set.starlev.starredheltix.client.StarredHeltixClient.CONFIG.hasShownVotingReminderToday = true;
            set.starlev.starredheltix.client.StarredHeltixClient.CONFIG.save();
        }
    }



    private static void showReminder(MinecraftClient client) {
        ClientPlayerEntity player = client.player;

        // Создаем кликабельное сообщение
        MutableText message = Text.literal(REMINDER_MESSAGE)
            .setStyle(Style.EMPTY
                .withColor(Formatting.YELLOW)
                .withUnderline(true)
                .withBold(true)
                .withClickEvent(new ClickEvent.RunCommand(VOTE_COMMAND))
                .withHoverEvent(new HoverEvent.ShowText(Text.literal("Нажмите чтобы проголосовать"))));

        assert player != null;
        player.sendMessage(message, false);

        // Показываем title и subtitle
        MutableText title = Text.literal(TITLE_MESSAGE)
            .setStyle(Style.EMPTY.withColor(Formatting.GOLD).withBold(true));

        MutableText subtitle = Text.literal(SUBTITLE_MESSAGE)
            .setStyle(Style.EMPTY.withColor(Formatting.YELLOW));

        client.inGameHud.setTitle(title);
        client.inGameHud.setSubtitle(subtitle);
        
        // Проигрываем кастомный звук уведомления
        client.getSoundManager().play(PositionedSoundInstance.master(ModSounds.VOTING_REMINDER, 1.0F, 1.0F));
    }

    public static void markAsVoted() {
        set.starlev.starredheltix.client.StarredHeltixClient.CONFIG.hasVotedToday = true;
        set.starlev.starredheltix.client.StarredHeltixClient.CONFIG.save();

        // Показываем сообщение благодарности
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.player != null) {
            MutableText thankYou = Text.literal(THANK_YOU_MESSAGE)
                .setStyle(Style.EMPTY.withColor(Formatting.GREEN).withBold(true));
            client.player.sendMessage(thankYou, false);
        }
    }

    public static boolean hasVotedToday() {
        return set.starlev.starredheltix.client.StarredHeltixClient.CONFIG.hasVotedToday;
    }



    // Проверяем, была ли введена команда голосования
    public static void onVoteCommand() {
        markAsVoted();
    }

    // Включить/выключить напоминания
    public static void setVotingReminderEnabled(boolean enabled) {
        votingReminderEnabled = enabled;
    }

    // Получить статус напоминаний
    public static boolean isVotingReminderEnabled() {
        return votingReminderEnabled;
    }

    // Принудительный сброс дня
    public static void forceDayReset() {
        set.starlev.starredheltix.client.StarredHeltixClient.CONFIG.lastVotingDate = "";
        set.starlev.starredheltix.client.StarredHeltixClient.CONFIG.hasVotedToday = false;
        set.starlev.starredheltix.client.StarredHeltixClient.CONFIG.save();
    }
}