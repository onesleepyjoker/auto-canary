package com.iscas.autoCanary.pojo.output;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ImageOutput{
    /**
     * 镜像名称
     */
    private String imageName;

    /**
     * 版本
     */
    private String version;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImageOutput that = (ImageOutput) o;
        return Objects.equals(imageName, that.imageName) && Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(imageName, version);
    }

}
