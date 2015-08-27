package com.hortonworks.iotas.parsers.nest;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Paths;

public class Thermostat {
    private String device_id;
    private String locale;
    private String software_version;
    private String structure_id;
    private String name;
    private String name_long;
    private String last_connection;
    private Boolean is_online;
    private Boolean can_cool;
    private Boolean can_heat;
    private Boolean is_using_emergency_heat;
    private Boolean has_fan;
    private Boolean fan_timer_active;
    private String fan_timer_timeout;
    private Boolean has_leaf;
    private String temperature_scale;
    private Integer target_temperature_f;
    private Integer target_temperature_c;
    private Integer target_temperature_high_f;
    private Integer target_temperature_high_c;
    private Integer target_temperature_low_f;
    private Integer target_temperature_low_c;
    private Integer away_temperature_high_f;
    private Integer away_temperature_high_c;
    private Integer away_temperature_low_f;
    private Integer away_temperature_low_c;
    private String hvac_mode;
    private Integer ambient_temperature_f;
    private Integer ambient_temperature_c;
    private Integer humidity;
    private String hvac_state;

    public Thermostat() {
    }

    private String where_id;

    public String getDevice_id() {
        return device_id;
    }

    public void setDevice_id(String device_id) {
        this.device_id = device_id;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getSoftware_version() {
        return software_version;
    }

    public void setSoftware_version(String software_version) {
        this.software_version = software_version;
    }

    public String getStructure_id() {
        return structure_id;
    }

    public void setStructure_id(String structure_id) {
        this.structure_id = structure_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName_long() {
        return name_long;
    }

    public void setName_long(String name_long) {
        this.name_long = name_long;
    }

    public String getLast_connection() {
        return last_connection;
    }

    public void setLast_connection(String last_connection) {
        this.last_connection = last_connection;
    }

    public Boolean getIs_online() {
        return is_online;
    }

    public void setIs_online(Boolean is_online) {
        this.is_online = is_online;
    }

    public Boolean getCan_cool() {
        return can_cool;
    }

    public void setCan_cool(Boolean can_cool) {
        this.can_cool = can_cool;
    }

    public Boolean getCan_heat() {
        return can_heat;
    }

    public void setCan_heat(Boolean can_heat) {
        this.can_heat = can_heat;
    }

    public Boolean getIs_using_emergency_heat() {
        return is_using_emergency_heat;
    }

    public void setIs_using_emergency_heat(Boolean is_using_emergency_heat) {
        this.is_using_emergency_heat = is_using_emergency_heat;
    }

    public Boolean getHas_fan() {
        return has_fan;
    }

    public void setHas_fan(Boolean has_fan) {
        this.has_fan = has_fan;
    }

    public Boolean getFan_timer_active() {
        return fan_timer_active;
    }

    public void setFan_timer_active(Boolean fan_timer_active) {
        this.fan_timer_active = fan_timer_active;
    }

    public String getFan_timer_timeout() {
        return fan_timer_timeout;
    }

    public void setFan_timer_timeout(String fan_timer_timeout) {
        this.fan_timer_timeout = fan_timer_timeout;
    }

    public Boolean getHas_leaf() {
        return has_leaf;
    }

    public void setHas_leaf(Boolean has_leaf) {
        this.has_leaf = has_leaf;
    }

    public String getTemperature_scale() {
        return temperature_scale;
    }

    public void setTemperature_scale(String temperature_scale) {
        this.temperature_scale = temperature_scale;
    }

    public Integer getTarget_temperature_f() {
        return target_temperature_f;
    }

    public void setTarget_temperature_f(Integer target_temperature_f) {
        this.target_temperature_f = target_temperature_f;
    }

    public Integer getTarget_temperature_c() {
        return target_temperature_c;
    }

    public void setTarget_temperature_c(Integer target_temperature_c) {
        this.target_temperature_c = target_temperature_c;
    }

    public Integer getTarget_temperature_high_f() {
        return target_temperature_high_f;
    }

    public void setTarget_temperature_high_f(Integer target_temperature_high_f) {
        this.target_temperature_high_f = target_temperature_high_f;
    }

    public Integer getTarget_temperature_high_c() {
        return target_temperature_high_c;
    }

    public void setTarget_temperature_high_c(Integer target_temperature_high_c) {
        this.target_temperature_high_c = target_temperature_high_c;
    }

    public Integer getTarget_temperature_low_f() {
        return target_temperature_low_f;
    }

    public void setTarget_temperature_low_f(Integer target_temperature_low_f) {
        this.target_temperature_low_f = target_temperature_low_f;
    }

    public Integer getTarget_temperature_low_c() {
        return target_temperature_low_c;
    }

    public void setTarget_temperature_low_c(Integer target_temperature_low_c) {
        this.target_temperature_low_c = target_temperature_low_c;
    }

    public Integer getAway_temperature_high_f() {
        return away_temperature_high_f;
    }

    public void setAway_temperature_high_f(Integer away_temperature_high_f) {
        this.away_temperature_high_f = away_temperature_high_f;
    }

    public Integer getAway_temperature_high_c() {
        return away_temperature_high_c;
    }

    public void setAway_temperature_high_c(Integer away_temperature_high_c) {
        this.away_temperature_high_c = away_temperature_high_c;
    }

    public Integer getAway_temperature_low_f() {
        return away_temperature_low_f;
    }

    public void setAway_temperature_low_f(Integer away_temperature_low_f) {
        this.away_temperature_low_f = away_temperature_low_f;
    }

    public Integer getAway_temperature_low_c() {
        return away_temperature_low_c;
    }

    public void setAway_temperature_low_c(Integer away_temperature_low_c) {
        this.away_temperature_low_c = away_temperature_low_c;
    }

    public String getHvac_mode() {
        return hvac_mode;
    }

    public void setHvac_mode(String hvac_mode) {
        this.hvac_mode = hvac_mode;
    }

    public Integer getAmbient_temperature_f() {
        return ambient_temperature_f;
    }

    public void setAmbient_temperature_f(Integer ambient_temperature_f) {
        this.ambient_temperature_f = ambient_temperature_f;
    }

    public Integer getAmbient_temperature_c() {
        return ambient_temperature_c;
    }

    public void setAmbient_temperature_c(Integer ambient_temperature_c) {
        this.ambient_temperature_c = ambient_temperature_c;
    }

    public Integer getHumidity() {
        return humidity;
    }

    public void setHumidity(Integer humidity) {
        this.humidity = humidity;
    }

    public String getHvac_state() {
        return hvac_state;
    }

    public void setHvac_state(String hvac_state) {
        this.hvac_state = hvac_state;
    }

    public String getWhere_id() {
        return where_id;
    }

    public void setWhere_id(String where_id) {
        this.where_id = where_id;
    }

    @Override
    public String toString() {
        return "Thermostat{" +
                "device_id='" + device_id + '\'' +
                ", locale='" + locale + '\'' +
                ", software_version='" + software_version + '\'' +
                ", structure_id='" + structure_id + '\'' +
                ", name='" + name + '\'' +
                ", name_long='" + name_long + '\'' +
                ", last_connection='" + last_connection + '\'' +
                ", is_online=" + is_online +
                ", can_cool=" + can_cool +
                ", can_heat=" + can_heat +
                ", is_using_emergency_heat=" + is_using_emergency_heat +
                ", has_fan=" + has_fan +
                ", fan_timer_active=" + fan_timer_active +
                ", fan_timer_timeout='" + fan_timer_timeout + '\'' +
                ", has_leaf=" + has_leaf +
                ", temperature_scale='" + temperature_scale + '\'' +
                ", target_temperature_f=" + target_temperature_f +
                ", target_temperature_c=" + target_temperature_c +
                ", target_temperature_high_f=" + target_temperature_high_f +
                ", target_temperature_high_c=" + target_temperature_high_c +
                ", target_temperature_low_f=" + target_temperature_low_f +
                ", target_temperature_low_c=" + target_temperature_low_c +
                ", away_temperature_high_f=" + away_temperature_high_f +
                ", away_temperature_high_c=" + away_temperature_high_c +
                ", away_temperature_low_f=" + away_temperature_low_f +
                ", away_temperature_low_c=" + away_temperature_low_c +
                ", hvac_mode='" + hvac_mode + '\'' +
                ", ambient_temperature_f=" + ambient_temperature_f +
                ", ambient_temperature_c=" + ambient_temperature_c +
                ", humidity=" + humidity +
                ", hvac_state='" + hvac_state + '\'' +
                ", where_id='" + where_id + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Thermostat)) return false;

        Thermostat that = (Thermostat) o;

        if (device_id != null ? !device_id.equals(that.device_id) : that.device_id != null) return false;
        if (locale != null ? !locale.equals(that.locale) : that.locale != null) return false;
        if (software_version != null ? !software_version.equals(that.software_version) : that.software_version != null)
            return false;
        if (structure_id != null ? !structure_id.equals(that.structure_id) : that.structure_id != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (name_long != null ? !name_long.equals(that.name_long) : that.name_long != null) return false;
        if (last_connection != null ? !last_connection.equals(that.last_connection) : that.last_connection != null)
            return false;
        if (is_online != null ? !is_online.equals(that.is_online) : that.is_online != null) return false;
        if (can_cool != null ? !can_cool.equals(that.can_cool) : that.can_cool != null) return false;
        if (can_heat != null ? !can_heat.equals(that.can_heat) : that.can_heat != null) return false;
        if (is_using_emergency_heat != null ? !is_using_emergency_heat.equals(that.is_using_emergency_heat) : that.is_using_emergency_heat != null)
            return false;
        if (has_fan != null ? !has_fan.equals(that.has_fan) : that.has_fan != null) return false;
        if (fan_timer_active != null ? !fan_timer_active.equals(that.fan_timer_active) : that.fan_timer_active != null)
            return false;
        if (fan_timer_timeout != null ? !fan_timer_timeout.equals(that.fan_timer_timeout) : that.fan_timer_timeout != null)
            return false;
        if (has_leaf != null ? !has_leaf.equals(that.has_leaf) : that.has_leaf != null) return false;
        if (temperature_scale != null ? !temperature_scale.equals(that.temperature_scale) : that.temperature_scale != null)
            return false;
        if (target_temperature_f != null ? !target_temperature_f.equals(that.target_temperature_f) : that.target_temperature_f != null)
            return false;
        if (target_temperature_c != null ? !target_temperature_c.equals(that.target_temperature_c) : that.target_temperature_c != null)
            return false;
        if (target_temperature_high_f != null ? !target_temperature_high_f.equals(that.target_temperature_high_f) : that.target_temperature_high_f != null)
            return false;
        if (target_temperature_high_c != null ? !target_temperature_high_c.equals(that.target_temperature_high_c) : that.target_temperature_high_c != null)
            return false;
        if (target_temperature_low_f != null ? !target_temperature_low_f.equals(that.target_temperature_low_f) : that.target_temperature_low_f != null)
            return false;
        if (target_temperature_low_c != null ? !target_temperature_low_c.equals(that.target_temperature_low_c) : that.target_temperature_low_c != null)
            return false;
        if (away_temperature_high_f != null ? !away_temperature_high_f.equals(that.away_temperature_high_f) : that.away_temperature_high_f != null)
            return false;
        if (away_temperature_high_c != null ? !away_temperature_high_c.equals(that.away_temperature_high_c) : that.away_temperature_high_c != null)
            return false;
        if (away_temperature_low_f != null ? !away_temperature_low_f.equals(that.away_temperature_low_f) : that.away_temperature_low_f != null)
            return false;
        if (away_temperature_low_c != null ? !away_temperature_low_c.equals(that.away_temperature_low_c) : that.away_temperature_low_c != null)
            return false;
        if (hvac_mode != null ? !hvac_mode.equals(that.hvac_mode) : that.hvac_mode != null) return false;
        if (ambient_temperature_f != null ? !ambient_temperature_f.equals(that.ambient_temperature_f) : that.ambient_temperature_f != null)
            return false;
        if (ambient_temperature_c != null ? !ambient_temperature_c.equals(that.ambient_temperature_c) : that.ambient_temperature_c != null)
            return false;
        if (humidity != null ? !humidity.equals(that.humidity) : that.humidity != null) return false;
        if (hvac_state != null ? !hvac_state.equals(that.hvac_state) : that.hvac_state != null) return false;
        return !(where_id != null ? !where_id.equals(that.where_id) : that.where_id != null);

    }

    @Override
    public int hashCode() {
        int result = device_id != null ? device_id.hashCode() : 0;
        result = 31 * result + (locale != null ? locale.hashCode() : 0);
        result = 31 * result + (software_version != null ? software_version.hashCode() : 0);
        result = 31 * result + (structure_id != null ? structure_id.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (name_long != null ? name_long.hashCode() : 0);
        result = 31 * result + (last_connection != null ? last_connection.hashCode() : 0);
        result = 31 * result + (is_online != null ? is_online.hashCode() : 0);
        result = 31 * result + (can_cool != null ? can_cool.hashCode() : 0);
        result = 31 * result + (can_heat != null ? can_heat.hashCode() : 0);
        result = 31 * result + (is_using_emergency_heat != null ? is_using_emergency_heat.hashCode() : 0);
        result = 31 * result + (has_fan != null ? has_fan.hashCode() : 0);
        result = 31 * result + (fan_timer_active != null ? fan_timer_active.hashCode() : 0);
        result = 31 * result + (fan_timer_timeout != null ? fan_timer_timeout.hashCode() : 0);
        result = 31 * result + (has_leaf != null ? has_leaf.hashCode() : 0);
        result = 31 * result + (temperature_scale != null ? temperature_scale.hashCode() : 0);
        result = 31 * result + (target_temperature_f != null ? target_temperature_f.hashCode() : 0);
        result = 31 * result + (target_temperature_c != null ? target_temperature_c.hashCode() : 0);
        result = 31 * result + (target_temperature_high_f != null ? target_temperature_high_f.hashCode() : 0);
        result = 31 * result + (target_temperature_high_c != null ? target_temperature_high_c.hashCode() : 0);
        result = 31 * result + (target_temperature_low_f != null ? target_temperature_low_f.hashCode() : 0);
        result = 31 * result + (target_temperature_low_c != null ? target_temperature_low_c.hashCode() : 0);
        result = 31 * result + (away_temperature_high_f != null ? away_temperature_high_f.hashCode() : 0);
        result = 31 * result + (away_temperature_high_c != null ? away_temperature_high_c.hashCode() : 0);
        result = 31 * result + (away_temperature_low_f != null ? away_temperature_low_f.hashCode() : 0);
        result = 31 * result + (away_temperature_low_c != null ? away_temperature_low_c.hashCode() : 0);
        result = 31 * result + (hvac_mode != null ? hvac_mode.hashCode() : 0);
        result = 31 * result + (ambient_temperature_f != null ? ambient_temperature_f.hashCode() : 0);
        result = 31 * result + (ambient_temperature_c != null ? ambient_temperature_c.hashCode() : 0);
        result = 31 * result + (humidity != null ? humidity.hashCode() : 0);
        result = 31 * result + (hvac_state != null ? hvac_state.hashCode() : 0);
        result = 31 * result + (where_id != null ? where_id.hashCode() : 0);
        return result;
    }

    public static void main (String[] args) throws Exception {
        String data = Files.readAllLines(Paths.get("/Users/pbrahmbhatt/repo/IoTaS/parsers/src/main/java/com/hortonworks/iotas/parsers/nest/data")).get(0);
        Thermostat thermostat = new ObjectMapper().readValue(data, Thermostat.class);
        System.out.println(thermostat);
    }
}
