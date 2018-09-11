#!/bin/bash

export TEMP_SENSOR_TOPIC=/NODE-4AD7F2/temp
export THERMO_CURRENT_TEMP_TOPIC=/loungeThermostat/currentTemp
export THERMO_TARGET_TEMP_TOPIC=/loungeThermostat/targetTemp
export THERMO_GET_TARGET_TEMP_TOPIC=/loungeThermostat/getTargetTemp
export CURRENT_HEATING_COOLING_TOPIC=/loungeThermostat/currentHeatingCoolingState
export HEATER_TOPICS=homeassistant/switch/192_168_1_50/command,homeassistant/switch/192_168_1_52/command
export CLIENT_ID="Lounge Thermostat"
./sbt run
