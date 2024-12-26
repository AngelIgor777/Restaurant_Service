package org.test.restaurant_service.telegram.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendSticker;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.test.restaurant_service.entity.Otp;
import org.test.restaurant_service.service.impl.OtpServiceImpl;
import org.test.restaurant_service.telegram.config.BotConfig;

import javax.persistence.EntityNotFoundException;
import java.security.SecureRandom;
import java.util.ArrayList;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    private final OtpServiceImpl otpService;


    private final String helpText = "\uD83D\uDCDA Доступные команды:\n" +
            "/start - Запуск бота\n" +
            "/help - Список доступных команд\n" +
            "/register - Регистрация на нашем сайте\n" +
            "/info - Информация о боте\n" +
            "/menu - Показать меню";

    private final String menuText = "\uD83C\uDF74 Наше меню:\n" +
            "1. Салаты\n" +
            "2. Основные блюда\n" +
            "3. Десерты\n" +
            "4. Напитки\n\n" +
            "Введите номер категории, чтобы узнать больше!";


    private final BotConfig botConfig;
    private final SecureRandom random = new SecureRandom();

    public TelegramBot(OtpServiceImpl otpService, BotConfig botConfig) {
        this.otpService = otpService;

        this.botConfig = botConfig;
        ArrayList<BotCommand> botCommands = new ArrayList<>();
        botCommands.add(new BotCommand("/start", "Запуск бота"));
        botCommands.add(new BotCommand("/help", "Список доступных команд"));
        botCommands.add(new BotCommand("/register", "Регистрация"));
        botCommands.add(new BotCommand("/info", "Информация о боте"));
        botCommands.add(new BotCommand("/menu", "Показать меню"));


        try {
            this.execute(new SetMyCommands(botCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        String botName = botConfig.getBotName();
        log.debug("BOT NAME: {}", botName);

        return botName;
    }

    @Override
    public String getBotToken() {
        String botKey = botConfig.getBotKey();
        log.debug("BOT KEY: {}", botKey);
        return botKey;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();
            User user = update.getMessage().getFrom();
            String userName = user.getUserName();
            String firstName = user.getFirstName();
            String lastName = user.getLastName();
            log.info("USER: {}, {}, {}", userName, firstName, lastName);

            switch (text) {
                case "/start":
                    startRegister(chatId, user);
                    break;
                case "/help":
                    sendHelpMessage(chatId);
                    break;
                case "/register":
                    registerOtpCode(chatId);
                    break;
                case "/info":
                    sendMessage(chatId, "Этот бот помогает вам зарегистрироваться и получать новости о мероприятиях ARNAUT's! ☀");
                    break;
                case "/menu":
                    sendMenu(chatId);
                    break;
                default:
                    sendMessage(chatId, "Неизвестная команда 🤯. Введите /help, чтобы увидеть доступные команды.");
                    break;
            }
        } else if (update.getMessage().hasSticker()) {
            stickerHandler(update);
        }
    }

    private void stickerHandler(Update update) {
        Long chatId = update.getMessage().getChatId();
        String fileId = update.getMessage().getSticker().getFileId();
        sendMessage(chatId, "Какой прекрасный стикер! 🙃");
        log.info("Получен File ID стикера: {}", fileId);
    }

    private void sendHelpMessage(Long chatId) {

        sendMessage(chatId, helpText);
    }

    private void sendMenu(Long chatId) {

        sendMessage(chatId, menuText);
    }

    private void startRegister(Long chatId, User user) {
        if (!otpService.existByChatId(chatId)) {
            sendSticker(chatId, "CAACAgIAAxkBAAOIZ2wCV5OzULOMka95E5_NGb48DX8AAocQAALddzlI382554aYWfM2BA");
            sendMessage(chatId, "Добро пожаловать в бот ресторана ARNAUT's! ☺ \n" +
                    "Введите /help, чтобы узнать, что я могу сделать.");
            otpService.save(chatId, user);
        } else {
            sendMessage(chatId, "Ой, вышла ошибочка 😅.\n" +
                    "Мы заметили, что вы уже запустили нашего бота 😽.\n" +
                    "Можете ввести /help, чтобы узнать, что я могу сделать. 😌");
        }
    }

    private void registerOtpCode(Long chatId) {
        if (otpService.existByChatId(chatId)) {
            sendSticker(chatId, "CAACAgIAAxkBAAOMZ2wCg2GLi8plYN0NGFsVl2NfnMYAAgsBAAL3AsgPxfQ7mJWqcds2BA");
            try {
                Otp otp = otpService.generateAndSaveOtp(chatId);

                String message = "Поздравляем! Теперь вы являетесь частью нашей семьи!\n" +
                        "🎉 Ваш код: `" + otp.getOtpCode() + "` 🎉\n" +
                        "🔒 Никому не давайте его.\n" +
                        "🌐 Заходите на наш сайт и регистрируйтесь с помощью этого кода!\n" +
                        "🎁 Вы сможете участвовать в розыгрышах, получать промокоды и видеть новости самыми первыми!";

                sendMessageWithMarkdown(chatId, message);
            } catch (EntityNotFoundException e) {
                sendMessage(chatId, "Ой, вышла ошибочка 😅.\n" +
                        "Мы заметили, что вы уже есть в нашем списке.\n" +
                        "Введите /me чтобы получить персональную информацию 😌");
            }

        }
    }


    private void sendMessageWithMarkdown(Long chatId, String message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId.toString());
        sendMessage.setText(message);
        sendMessage.setParseMode("Markdown"); // Использование Markdown для форматирования текста
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения: {}", e.getMessage());
        }
    }


    private void sendSticker(Long chatId, String stickerFileId) {
        SendSticker sendSticker = new SendSticker();
        sendSticker.setChatId(chatId.toString());
        sendSticker.setSticker(new InputFile(stickerFileId)); // Используем File ID стикера или URL

        try {
            execute(sendSticker);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке стикера: {}", e.getMessage());
        }
    }


    private void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения: {}", e.getMessage());
        }
    }


}
