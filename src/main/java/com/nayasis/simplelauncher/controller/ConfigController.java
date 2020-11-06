package com.nayasis.simplelauncher.controller;

import com.nayasis.simplelauncher.jpa.entity.ConfigEntity;
import com.nayasis.simplelauncher.jpa.repository.ConfigRepository;
import com.nayasis.simplelauncher.vo.RestoreConfig;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;

import static com.nayasis.simplelauncher.common.CONSTANT.STAGE.MAIN;

@Component
@Slf4j
public class ConfigController {

	@Autowired
	private MainController mainController;

	@Autowired
	private ConfigRepository configRepository;

	private final String CONFIG_MAIN = "CONFIG_MAIN";

    @Transactional
	public void save() {

		RestoreConfig config = new RestoreConfig();

		config.setMainStageProperties( MAIN.getStageProperties() );
		config.setFocusedRow( mainController.tableMain.getFocusedIndex() );

		ConfigEntity entity = configRepository.findByKey( CONFIG_MAIN );
		if( entity == null ) {
			entity = new ConfigEntity( CONFIG_MAIN );
		}

		entity.setValue( config.serialize() );

		configRepository.save( entity );

	}

	public void restoreMainStageProperties() {

		RestoreConfig config = getConfig();
		if ( config == null ) return;

		log.trace( ">> bind stage property");

		try {
			MAIN.setStageProperties( config.getMainStageProperties() );
		} catch ( Exception e ) {
			log.error( e.getMessage(), e );
		}

		mainController.showDescription( mainController.menuitemViewDesc.isSelected() );
		mainController.showMenuBar( mainController.menuitemViewMenuBar.isSelected() );
		mainController.alwaysOnTop( mainController.menuitemAlwaysOnTop.isSelected() );

	}

	public void restoreMainTableFocus() {

		RestoreConfig config = getConfig();
		if ( config == null ) return;

		if( config.getFocusedRow() != 0 ) {
			mainController.tableMain.getSelectionModel().select( config.getFocusedRow() );
		}

	}

	@Nullable
	public RestoreConfig getConfig() {
		ConfigEntity entity = configRepository.findByKey( CONFIG_MAIN );
		return ( entity == null ) ? null : new RestoreConfig( entity.getValue() );
	}

}
