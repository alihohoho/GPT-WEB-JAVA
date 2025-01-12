package com.intelligent.bot.service.mj.impl;

import com.intelligent.bot.api.midjourney.support.TaskCondition;
import com.intelligent.bot.base.exception.E;
import com.intelligent.bot.model.MjTask;
import com.intelligent.bot.service.mj.TaskStoreService;
import com.intelligent.bot.service.sys.IMjTaskService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@Transactional(rollbackFor = E.class)
public class TaskStoreServiceImpl implements TaskStoreService {

	@Resource
	IMjTaskService mjTaskService;

	@Override
	public void saveTask(MjTask task) {
		mjTaskService.saveOrUpdate(task);
	}

	@Override
	public void deleteTask(Long id) {
		mjTaskService.removeById(id);
	}

	@Override
	public MjTask getTask(Long id) {
		return mjTaskService.getById(id);
	}

	@Override
	public MjTask findOne(TaskCondition taskCondition) {
		return mjTaskService.list().stream().filter(taskCondition).findFirst().orElse(null);
	}



}
