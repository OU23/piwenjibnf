package com.asset.management.storage;

import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Set;

/**
 * 文件归属范围。
 *
 * <p>借鉴 Network-Drive 的 listType 设计（个人 / 部门 / 公共），但简化为三级：
 * 文件先按归属范围分开放，再按部门和业务检索。</p>
 */
public final class StorageScope {

    /** 个人文件：仅上传者可见 */
    public static final String PERSONAL = "personal";

    /** 部门文件：本部门可见（对应 sys_user.department） */
    public static final String DEPT = "dept";

    /** 公共文件：全员可见，如系统图标、模板 */
    public static final String PUBLIC = "public";

    private static final Set<String> ALL = Set.of(PERSONAL, DEPT, PUBLIC);

    private StorageScope() {
    }

    /** 归一化归属范围，非法值一律回落到 public */
    public static String normalize(String scope) {
        if (!StringUtils.hasText(scope)) {
            return PUBLIC;
        }
        String value = scope.trim().toLowerCase(Locale.ROOT);
        return ALL.contains(value) ? value : PUBLIC;
    }
}
