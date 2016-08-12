/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.eagle.security.auditlog.timer;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.eagle.common.config.EagleConfigConstants;
import org.apache.eagle.security.service.HdfsSensitivityEntity;
import org.apache.eagle.security.service.IMetadataServiceClient;
import org.apache.eagle.security.service.MetadataServiceClientImpl;
import org.apache.eagle.security.util.ExternalDataCache;
import org.apache.eagle.security.entity.FileSensitivityAPIEntity;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.Maps;

public class FileSensitivityPollingJob implements Job{
	private final static Logger LOG = LoggerFactory.getLogger(FileSensitivityPollingJob.class);
	
	@Override
	public void execute(JobExecutionContext context)
			throws JobExecutionException {
		JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
		try{
			Collection<HdfsSensitivityEntity> sensitivityAPIEntities = load(jobDataMap);
			if(sensitivityAPIEntities == null){
				LOG.warn("File sensitivity information is empty");
				return;
			}
			Map<String, HdfsSensitivityEntity> map = Maps.uniqueIndex(sensitivityAPIEntities, new Function<HdfsSensitivityEntity, String>(){
				@Override
				public String apply(HdfsSensitivityEntity input) {
					return input.getFiledir();
				}
			});
			ExternalDataCache.getInstance().setJobResult(getClass(), map);
		}catch(Exception ex){
			LOG.error("Fail loading file sensitivity data", ex);
		}
	}

	private Collection<HdfsSensitivityEntity> load(JobDataMap jobDataMap) throws Exception{
		Map<String, Object> map = (Map<String,Object>)jobDataMap.get(EagleConfigConstants.EAGLE_SERVICE);
		String eagleServiceHost = (String)map.get(EagleConfigConstants.HOST);
		Integer eagleServicePort = Integer.parseInt(map.get(EagleConfigConstants.PORT).toString());
		String username = map.containsKey(EagleConfigConstants.USERNAME) ? (String)map.get(EagleConfigConstants.USERNAME) : null;
		String password = map.containsKey(EagleConfigConstants.PASSWORD) ? (String)map.get(EagleConfigConstants.PASSWORD) : null;
		// load from eagle database
		LOG.info("Load file sensitivity information from eagle service " + eagleServiceHost + ":" + eagleServicePort);
		IMetadataServiceClient client = new MetadataServiceClientImpl(eagleServiceHost, eagleServicePort, "/rest");
		return client.listHdfsSensitivities();
	}
}
