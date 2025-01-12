package com.intelligent.bot.server.wss.user;


import com.intelligent.bot.constant.CommonConst;
import com.intelligent.bot.enums.mj.MessageType;
import com.intelligent.bot.model.SysConfig;
import com.intelligent.bot.server.wss.handle.MessageHandler;
import com.intelligent.bot.utils.sys.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
public class UserMessageListener implements ApplicationListener<ApplicationStartedEvent> {
	private final List<MessageHandler> messageHandlers = new ArrayList<>();

	@Override
	public void onApplicationEvent(ApplicationStartedEvent event) {
		this.messageHandlers.addAll(event.getApplicationContext().getBeansOfType(MessageHandler.class).values());
	}

	public void onMessage(DataObject raw) throws IOException {
		MessageType messageType = MessageType.of(raw.getString("t"));
		if (messageType == null) {
			return;
		}
		DataObject data = raw.getObject("d");
		if (ignoreAndLogMessage(data, messageType)) {
			return;
		}
		for (MessageHandler messageHandler : this.messageHandlers) {
			messageHandler.handle(messageType, data);
		}
	}

	private boolean ignoreAndLogMessage(DataObject data, MessageType messageType) {
		SysConfig sysConfig = RedisUtil.getCacheObject(CommonConst.SYS_CONFIG);

		String channelId = data.getString("channel_id");
		if (!sysConfig.getMjChannelId().equals(channelId)) {
			return true;
		}
		Optional<DataObject> author = data.optObject("author");
		if (!author.isPresent()) {
			return true;
		}
		String authorName = author.get().getString("username");
		log.debug("{} - {}: {}", messageType.name(), authorName, data.getString("content"));
		return !sysConfig.getMjBotName().equals(authorName);
	}
}