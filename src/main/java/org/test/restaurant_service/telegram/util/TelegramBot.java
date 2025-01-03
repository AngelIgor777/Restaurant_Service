package org.test.restaurant_service.telegram.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendSticker;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.test.restaurant_service.dto.response.ProductResponseDTO;
import org.test.restaurant_service.dto.response.ProductTypeResponseDTO;
import org.test.restaurant_service.entity.Otp;
import org.test.restaurant_service.service.impl.OtpServiceImpl;
import org.test.restaurant_service.service.impl.ProductServiceImpl;
import org.test.restaurant_service.service.impl.ProductTypeServiceImpl;
import org.test.restaurant_service.telegram.config.BotConfig;

import javax.persistence.EntityNotFoundException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@EnableScheduling

public class TelegramBot extends TelegramLongPollingBot {

    private final OtpServiceImpl otpService;
    private final ProductTypeServiceImpl productTypeService;
    private final ProductServiceImpl productService;


    private final String BUTTON_BACK_TO_MENU = "BACK_TO_MENU";

    private final String helpText =
            "📖 <b>Доступные команды:</b>\n\n" +
                    "🚀 /start - <i>Запуск бота</i>\n" +
                    "❓ /help - <i>Список доступных команд</i>\n" +
                    "📝 /register - <i>Регистрация на нашем сайте</i>\n" +
                    "ℹ️ /info - <i>Информация о боте</i>\n" +
                    "🍽️ /menu - <i>Показать меню</i>\n\n" +
                    "✨ Используйте команды, чтобы сделать ваше настроение более радостным!";
    private final String infoText =
            "🤖 <b>Добро пожаловать!</b>\n\n" +
                    "Этот бот создан, чтобы сделать вашу жизнь проще и приятнее! 🌟\n" +
                    "С его помощью вы можете:\n" +
                    "📝 Зарегистрироваться на нашем сайте\n" +
                    "📢 Получать самые свежие новости о мероприятиях ARNAUT's\n\n" +
                    "✨ Мы всегда рады быть вам полезными!";
    private final BotConfig botConfig;

    private final ProductServiceImpl productServiceImpl;
    private final OtpServiceImpl otpServiceImpl;

    private List<String> callbackProductTypesData = new ArrayList<>();

    private List<String> callbackProductsData = new ArrayList<>();


    public TelegramBot(OtpServiceImpl otpService, ProductTypeServiceImpl productTypeService, ProductServiceImpl productService, BotConfig botConfig, ProductServiceImpl productServiceImpl, OtpServiceImpl otpServiceImpl) {
        this.otpService = otpService;
        this.productTypeService = productTypeService;
        this.productService = productService;

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
        this.productServiceImpl = productServiceImpl;
        this.otpServiceImpl = otpServiceImpl;
    }

    @Override
    public String getBotUsername() {
        String botName = botConfig.getBotName();
        return botName;
    }

    @Override
    public String getBotToken() {
        String botKey = botConfig.getBotKey();
        return botKey;

    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText();

            switch (text) {
                case "/start":
                    startRegister(update);
                    break;
                case "/help":
                    sendHelpMessage(update);
                    break;
                case "/register":
                    registerOtpCode(update);
                    break;
                case "/info":
                    sendMessage(update, infoText);
                    break;
                case "/menu":
                    menu(update);
                    break;
                default:
                    sendMessage(update, "Неизвестная команда 🤯. Введите /help, чтобы увидеть доступные команды.");
                    break;
            }
        } else if (update.hasCallbackQuery()) {
            handleCallbackQuery(update);

        } else if (update.getMessage().hasSticker()) {
            stickerHandler(update);
        }
    }

    @Scheduled(cron = "${cron.scheduler}")
    private void sendAds() {
        String adText =
                "🍽️ <b>Время вкусных открытий!</b>\n\n" +
                        "✨ Сегодня у нас для вас нечто особенное:\n" +
                        "🍕 <b>Пицца недели:</b> Сырный взрыв — только 149 mdl!\n" +
                        "🍹 <b>Коктейли:</b> Закажи два и получи третий в подарок!\n\n" +
                        "🎉 <i>Забронируйте столик прямо сейчас, чтобы не упустить шанс насладиться уникальными блюдами!</i>\n\n" +
                        "📲 Нажмите /menu, чтобы посмотреть всё меню!\n\n" +
                        "❤️ С любовью, ваш ARNAUT's!";

        List<Otp> all = otpServiceImpl.getAll();
        for (Otp otp : all) {
            prepareAndSendMessage(otp.getChatId(), adText);
        }
    }

    private void prepareAndSendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        executeMessage(message);
    }

    private void handleCallbackQuery(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String data = callbackQuery.getData();

        boolean anyMatchProductTypes = callbackProductTypesData.stream()
                .anyMatch(callbackItem -> callbackItem.equals(data));
        boolean anyMatchProducts = callbackProductsData.stream()
                .anyMatch(callbackItem -> callbackItem.equals(data));


        if (anyMatchProductTypes) {
            handleProductTypeCallback(callbackQuery, data);
        }
        if (anyMatchProducts) {
            setToProduct(update, data);

        } else if (data.equals(BUTTON_BACK_TO_MENU)) {
            backToMenu(update);
        }
    }

    private void setToProduct(Update update, String product) {
        ProductResponseDTO productResponse = productServiceImpl.getByName(product);
        StringBuilder productText = getProductText(productResponse);
        EditMessageText editMessage = setEditMessageTextProperties(update);
        editMessage.setParseMode("HTML");

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> inlineKeyboardButtons = new ArrayList<>();

        editMessage.setText(productText.toString());

        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();


        inlineKeyboardButton.setText("Назад");
        inlineKeyboardButton.setCallbackData(productResponse.getTypeName());

        inlineKeyboardButtons.add(inlineKeyboardButton);
        rowsInLine.add(inlineKeyboardButtons);
        markupInLine.setKeyboard(rowsInLine);

        editMessage.setReplyMarkup(markupInLine);
        executeMessage(editMessage);
    }


    private StringBuilder getProductText(ProductResponseDTO productResponse) {
        StringBuilder productText = new StringBuilder();

        productText.append("🍴 <b>Блюдо:</b> ").append(productResponse.getName()).append("\n");
        productText.append("✨ <i>Описание:</i> ").append(productResponse.getDescription()).append("\n");
        productText.append("📂 <i>Категория:</i> ").append(productResponse.getTypeName()).append("\n");
        productText.append("💰 <b>Стоимость:</b> ").append(productResponse.getPrice()).append(" mdl\n");
        LocalTime cookingTime = productResponse.getCookingTime();
        if (cookingTime == null) {
            productText.append("⏱️ <b>Время приготовления:</b> <i>Моментально ☺</i>\n");
        } else {
            productText.append("⏱️ <b>Время приготовления:</b> ").append(cookingTime.getMinute()).append(" минут\n");
        }

        productText.append("\n🍽️ Наслаждайтесь изысканным вкусом и уютной атмосферой! ❤️");
        return productText;
    }

    private EditMessageText setEditMessageTextProperties(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        Long chatId = callbackQuery.getMessage().getChatId();
        Integer messageId = callbackQuery.getMessage().getMessageId();

        EditMessageText editMessage = new EditMessageText();
        editMessage.setChatId(String.valueOf(chatId));
        editMessage.setText(menuText.toString());
        editMessage.setMessageId((int) messageId);
        editMessage.setParseMode("HTML");

        return editMessage;
    }


    private void backToMenu(Update update) {

        EditMessageText editMessage = setEditMessageTextProperties(update);
        InlineKeyboardMarkup menuInlineMarkup = getMenuInlineMarkup();
        editMessage.setText(menuText.toString());
        editMessage.setReplyMarkup(menuInlineMarkup);
        executeMessage(editMessage);
        deleteMenuText();
    }

    private void handleProductTypeCallback(CallbackQuery callbackQuery, String productType) {
        List<ProductResponseDTO> products = productService.getByTypeName(productType); // TODO: Implement representation of all products in Telegram buttons

        Message message = callbackQuery.getMessage();
        Integer messageId = message.getMessageId();
        Long chatId = message.getChatId();

        String responseText = "🍽️ Вы выбрали категорию <b>" + productType + "</b>!\n\n" +
                "Можете нажать на блюдо, чтобы увидеть подробное описание.\n\n"
                + "Вот, что мы с любовью приготовили для вас 😋:\n";

        editMessageProductsByType(responseText, chatId, messageId, products);
    }

    private void editMessageProductsByType(String text, long chatId, long messageId, List<ProductResponseDTO> products) {
        EditMessageText message = new EditMessageText();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setMessageId((int) messageId);
        message.setParseMode("HTML");


        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();

        int size = products.size();
        int rows = (int) Math.ceil((double) size / 2);


        // Разбиваем на строки по 3 кнопки
        for (int i = 0; i < rows; i++) {
            List<InlineKeyboardButton> row = new ArrayList<>();

            // Индексы для кнопок в строке
            int limitation = Math.min((i + 2) * 2, size);
            for (int x = i * 3; x < limitation; x++) {
                InlineKeyboardButton button = createButton();
                String callbackData = products.get(x).getName();
                button.setText(callbackData);
                button.setCallbackData(callbackData);
                callbackProductsData.add(callbackData);
                row.add(button);
            }
            rowsInLine.add(row);
        }

        markupInLine.setKeyboard(rowsInLine);
        message.setReplyMarkup(markupInLine);


        addBackToMenuButton(rowsInLine);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    private void addBackToMenuButton(List<List<InlineKeyboardButton>> rowsInLine) {
        List<InlineKeyboardButton> row = new ArrayList<>();

        InlineKeyboardButton button = createButton();
        button.setText("Назад 🥞");
        button.setCallbackData(BUTTON_BACK_TO_MENU);
        row.add(button);
        rowsInLine.add(row);
    }


    private final StringBuilder menuText = new StringBuilder();

    private List<String> setMenuText() {
        List<String> productTypes = productTypeService.getAll().stream()
                .map(ProductTypeResponseDTO::getName).toList();
        int size = productTypes.size();
        menuText.append("🍽️ <i><b>Добро пожаловать в наше уютное меню!</b></i> \n \n")
                .append("✨ Здесь вы найдёте изысканные блюда, которые подарят вам наслаждение и радость! ✨\n\n");

        for (int i = 1; i <= size; i++) { // Исправлено: цикл с 1 до size включительно
            menuText.append("🍴 <b>")
                    .append(i).append(". ")
                    .append(productTypes.get(i - 1))
                    .append("\n</b>"); // Исправлено: корректный индекс
        }

        menuText.append("\n🎉 <i>Выберите номер категории, и мы поможем вам сделать лучший выбор!</i> 🎉\n")
                .append("💌 Спасибо, что выбираете нас! Ваш вкус — наша забота! 💌\n");
        return productTypes;
    }


    private void menu(Update update) {
        setMenuText();
        SendMessage message = new SendMessage(update.getMessage().getChatId().toString(), menuText.toString());
        message.setParseMode("HTML");
        createMenu(message);
    }

    private void createMenu(SendMessage message) {
        InlineKeyboardMarkup menuInlineMarkup = getMenuInlineMarkup();
        message.setReplyMarkup(menuInlineMarkup);
        executeMessage(message);
        deleteMenuText();
    }

    //нужно после каждой отправки menu
    private void deleteMenuText() {
        menuText.delete(0, menuText.length());
    }

    private InlineKeyboardMarkup getMenuInlineMarkup() {

        List<String> productTypes = setMenuText();

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();

        int size = productTypes.size();
        int rows = (int) Math.ceil((double) size / 2); // Округление вверх для правильного расчета строк

        // Разбиваем на строки по 2 кнопки
        for (int i = 0; i < rows; i++) {
            List<InlineKeyboardButton> row = new ArrayList<>();

            // Индексы для кнопок в строке
            int limitation = Math.min((i + 1) * 2, size);
            for (int x = i * 2; x < limitation; x++) {
                InlineKeyboardButton button = createButton();
                String callbackData = productTypes.get(x);
                button.setText(callbackData);
                button.setCallbackData(callbackData);
                callbackProductTypesData.add(callbackData);
                row.add(button);
            }
            rowsInLine.add(row);
        }

        markupInLine.setKeyboard(rowsInLine);
        return markupInLine;
    }

    private InlineKeyboardButton createButton() {
        return new InlineKeyboardButton();
    }


    private void executeMessage(SendMessage message) {
        message.setParseMode("HTML");
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    private void executeMessage(EditMessageText message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }


    private void stickerHandler(Update update) {
        String fileId = update.getMessage().getSticker().getFileId();
        sendMessage(update, "Какой прекрасный стикер! 🙃");
        log.info("Получен File ID стикера: {}", fileId);
    }

    private void sendHelpMessage(Update update) {

        sendMessage(update, helpText);
    }

    private void sendMenu(Update up) {

        sendMessage(up, menuText.toString());
    }

    private void startRegister(Update update) {
        Long chatId = update.getMessage().getChatId();
        User user = update.getMessage().getFrom();

        if (!otpService.existByChatId(chatId)) {
            sendSticker(chatId, "CAACAgIAAxkBAAOIZ2wCV5OzULOMka95E5_NGb48DX8AAocQAALddzlI382554aYWfM2BA");
            sendMessage(update, "Добро пожаловать в бот ресторана ARNAUT's! ☺ \n" +
                    "Введите /help, чтобы узнать, что я могу сделать.");
            otpService.save(chatId, user);
        } else {
            sendMessage(update, "Ой, вышла ошибочка 😅.\n" +
                    "Мы заметили, что вы уже запустили нашего бота 😽.\n" +
                    "Можете ввести /help, чтобы узнать, что я могу сделать. 😌");
        }
    }

    private void registerOtpCode(Update update) {

        String errorText = "Ой, вышла ошибочка 😅.\n" +
                "Мы заметили, что вы ещё не запустили нашего бота.\n" +
                "Введите /start чтобы всё получилось  😌";

        Long chatId = update.getMessage().getChatId();
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
                sendMessage(update, errorText);
            }
        } else {

            sendMessage(update, errorText);
        }
    }


    private void sendMessageWithMarkdown(Long chatId, String message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId.toString());
        sendMessage.setText(message);
        sendMessage.setParseMode("Markdown"); // Использование Markdown для форматирования текста
        executeMessage(sendMessage);
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


    private void sendMessage(Update update, String text) {
        SendMessage message = new SendMessage();
        message.setParseMode("HTML");
        message.setChatId(update.getMessage().getChatId().toString());
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения: {}", e.getMessage());
        }
    }


}
