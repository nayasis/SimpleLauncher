package com.nayasis.simplelauncher.controller;

import com.nayasis.simplelauncher.common.CONSTANT;
import com.nayasis.simplelauncher.jpa.entity.ConfigEntity;
import com.nayasis.simplelauncher.jpa.repository.ConfigRepository;
import io.nayasis.common.cache.implement.LruCache;
import io.nayasis.common.reflection.Reflector;
import io.nayasis.common.ui.javafx.properties.StageProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;

@Component
@Slf4j
public class ConfigController {

	@Autowired
	private ConfigRepository configRepository;

	private LruCache<String,String> keywordHistory = new LruCache<>( 20 );

	private enum ConfigKey {
		MAIN_STAGE, KEYWORD_HISTORY
	}

	public void restore() {
		restoreMainStageProperties();
		restoreKeywordHistory();
	}

	@Transactional
	public void save() {
		saveMainStageProperties();
		saveKeywordHistory();
	}

	private void restoreMainStageProperties() {

		ConfigEntity config = getConfig( ConfigKey.MAIN_STAGE );
		if( config == null ) return;

		StageProperties properties = Reflector.toBeanFrom( config.getValue(), StageProperties.class );

		CONSTANT.STAGE.MAIN.setConfigureProperties( properties );


	}

	public void saveMainStageProperties() {

		StageProperties properties = CONSTANT.STAGE.MAIN.getConfigureProperties();

		ConfigEntity config = getConfig( ConfigKey.MAIN_STAGE );
		if( config == null ) {
			config = new ConfigEntity( ConfigKey.MAIN_STAGE.name() );
		}

		config.setValue( Reflector.toJson(properties) );

		configRepository.save( config );

	}

	private void restoreKeywordHistory() {

		ConfigEntity config = getConfig( ConfigKey.KEYWORD_HISTORY );
		if( config == null ) return;

		keywordHistory.putAll( Reflector.toBeanFrom( config.getValue(), LruCache.class ) );

	}

	private void saveKeywordHistory() {

		ConfigEntity config = getConfig( ConfigKey.KEYWORD_HISTORY );
		if( config == null ) {
			config = new ConfigEntity( ConfigKey.KEYWORD_HISTORY.name() );
		}

		config.setValue( Reflector.toJson( keywordHistory ) );

		configRepository.save( config );

	}

	private ConfigEntity getConfig( ConfigKey key ) {
		return configRepository.findByKey( key.name() );
	}

	public LruCache<String, String> getKeywordHistory() {
		return keywordHistory;
	}

}
