package com.intelligent.bot.server.wss.handle;

import cn.hutool.core.text.CharSequenceUtil;
import com.intelligent.bot.api.midjourney.support.TaskCondition;
import com.intelligent.bot.enums.mj.MessageType;
import com.intelligent.bot.enums.mj.TaskAction;
import com.intelligent.bot.enums.mj.TaskStatus;
import com.intelligent.bot.model.MjTask;
import com.intelligent.bot.model.mj.data.UVContentParseData;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * upscale消息处理. todo: 待兼容blend
 * 开始(create): Upscaling image #1 with **[0152010266005012] cat** - <@1012983546824114217> (Waiting to start)
 * 进度: 无
 * 完成(create): **[0152010266005012] cat** - Image #1 <@1012983546824114217>
 * 完成-其他情况(create): **[5561516443317992] cat** - Upscaled by <@1083152202048217169> (fast)
 */
@Slf4j
@Component
public class UpscaleMessageHandler extends MessageHandler {
	private static final String START_CONTENT_REGEX = "Upscaling image #(\\d) with \\*\\*\\[(\\d+)\\] (.*?)\\*\\* - <@\\d+> \\((.*?)\\)";
	private static final String END_CONTENT_REGEX = "\\*\\*\\[(\\d+)\\] (.*?)\\*\\* - Image #(\\d) <@\\d+>";
	private static final String END2_CONTENT_REGEX = "\\*\\*\\[(\\d+)\\] (.*?)\\*\\* - Upscaled by <@\\d+> \\((.*?)\\)";

	@Override
	public void handle(MessageType messageType, DataObject message) throws IOException {
		if (MessageType.CREATE != messageType) {
			return;
		}
		String content = message.getString("content");
		UVContentParseData start = parseStart(content);
		if (start != null) {
			TaskCondition condition = new TaskCondition()
					.setRelatedTaskId(start.getTaskId())
					.setActionSet(Collections.singletonList(TaskAction.UPSCALE))
					.setStatusSet(Collections.singletonList(TaskStatus.SUBMITTED));
			MjTask task = this.taskQueueHelper.findRunningTask(condition)
					.filter(t -> CharSequenceUtil.endWith(t.getDescription(), "U" + start.getIndex()))
					.min(Comparator.comparing(MjTask::getSubmitTime))
					.orElse(null);
			if (task == null) {
				return;
			}
			task.setStatus(TaskStatus.IN_PROGRESS);
			task.awake();
			return;
		}
		UVContentParseData end = parseEnd(content);
		if (end != null) {
			TaskCondition condition = new TaskCondition()
					.setRelatedTaskId(end.getTaskId())
					.setActionSet(Collections.singletonList(TaskAction.UPSCALE))
					.setStatusSet(Collections.singletonList(TaskStatus.IN_PROGRESS));
			MjTask task = this.taskQueueHelper.findRunningTask(condition)
					.filter(t -> CharSequenceUtil.endWith(t.getDescription(), "U" + end.getIndex()))
					.min(Comparator.comparing(MjTask::getSubmitTime))
					.orElse(null);
			if (task == null) {
				return;
			}
			finishTask(task, message);
			task.awake();
			return;
		}
		UVContentParseData end2 = parseEnd2(content);
		if (end2 != null) {
			TaskCondition condition = new TaskCondition()
					.setRelatedTaskId(end2.getTaskId())
					.setActionSet(Collections.singletonList(TaskAction.UPSCALE))
					.setStatusSet(Collections.singletonList(TaskStatus.IN_PROGRESS));
			MjTask task = this.taskQueueHelper.findRunningTask(condition)
					.min(Comparator.comparing(MjTask::getSubmitTime))
					.orElse(null);
			if (task == null) {
				return;
			}
			finishTask(task, message);
			task.awake();
		}
	}

	@Override
	public void handle(MessageType messageType, Message message) throws IOException {
		if (MessageType.CREATE != messageType) {
			return;
		}
		String content = message.getContentRaw();
		UVContentParseData parseData = parseEnd(content);
		if (parseData != null) {
			TaskCondition condition = new TaskCondition()
					.setRelatedTaskId(parseData.getTaskId())
					.setActionSet(Collections.singletonList(TaskAction.UPSCALE))
					.setStatusSet(Arrays.asList(TaskStatus.SUBMITTED, TaskStatus.IN_PROGRESS));
			MjTask task = this.taskQueueHelper.findRunningTask(condition)
					.filter(t -> CharSequenceUtil.endWith(t.getDescription(), "U" + parseData.getIndex()))
					.min(Comparator.comparing(MjTask::getSubmitTime))
					.orElse(null);
			if (task == null) {
				return;
			}
			finishTask(task, message);
			task.awake();
			return;
		}
		UVContentParseData end2 = parseEnd2(content);
		if (end2 != null) {
			TaskCondition condition = new TaskCondition()
					.setRelatedTaskId(end2.getTaskId())
					.setActionSet(Collections.singletonList(TaskAction.UPSCALE))
					.setStatusSet(Arrays.asList(TaskStatus.SUBMITTED, TaskStatus.IN_PROGRESS));
			MjTask task = this.taskQueueHelper.findRunningTask(condition)
					.min(Comparator.comparing(MjTask::getSubmitTime))
					.orElse(null);
			if (task == null) {
				return;
			}
			finishTask(task, message);
			task.awake();
		}
	}

	private UVContentParseData parseStart(String content) {
		Matcher matcher = Pattern.compile(START_CONTENT_REGEX).matcher(content);
		if (!matcher.find()) {
			return null;
		}
		UVContentParseData parseData = new UVContentParseData();
		parseData.setIndex(Integer.parseInt(matcher.group(1)));
		parseData.setTaskId(Long.valueOf(matcher.group(2)));
		parseData.setPrompt(matcher.group(3));
		parseData.setStatus(matcher.group(4));
		return parseData;
	}

	private UVContentParseData parseEnd(String content) {
		Matcher matcher = Pattern.compile(END_CONTENT_REGEX).matcher(content);
		if (!matcher.find()) {
			return null;
		}
		UVContentParseData parseData = new UVContentParseData();
		parseData.setTaskId(Long.valueOf(matcher.group(1)));
		parseData.setPrompt(matcher.group(2));
		parseData.setIndex(Integer.parseInt(matcher.group(3)));
		parseData.setStatus("done");
		return parseData;
	}

	private UVContentParseData parseEnd2(String content) {
		Matcher matcher = Pattern.compile(END2_CONTENT_REGEX).matcher(content);
		if (!matcher.find()) {
			return null;
		}
		UVContentParseData parseData = new UVContentParseData();
		parseData.setTaskId(Long.valueOf(matcher.group(1)));
		parseData.setPrompt(matcher.group(2));
		parseData.setStatus(matcher.group(3));
		return parseData;
	}

}