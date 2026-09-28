package com.example.bookmanage.mapper;

import com.example.bookmanage.entity.SiteSetting;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 站点展示配置数据访问。
 */
@Mapper
public interface SiteSettingMapper {

    // 接口字段隐含 public static final，不能加 private 修饰
    String COLUMNS =
            "setting_key, setting_value, remark, updated_by, updated_at";

    @Select("SELECT " + COLUMNS + " FROM sys_site_setting")
    List<SiteSetting> selectAll();

    @Select("SELECT " + COLUMNS + " FROM sys_site_setting WHERE setting_key = #{key}")
    SiteSetting selectByKey(@Param("key") String key);

    /**
     * 首次写入与后续修改合并为一条语句：配置项是稀疏的，
     * 若区分 insert / update，调用方就得先查一次再决定分支，且并发下会出现重复插入。
     */
    @Insert("INSERT INTO sys_site_setting (setting_key, setting_value, remark, updated_by)"
            + " VALUES (#{settingKey}, #{settingValue}, #{remark}, #{updatedBy})"
            + " ON DUPLICATE KEY UPDATE setting_value = #{settingValue}, updated_by = #{updatedBy}")
    void upsert(SiteSetting setting);
}
