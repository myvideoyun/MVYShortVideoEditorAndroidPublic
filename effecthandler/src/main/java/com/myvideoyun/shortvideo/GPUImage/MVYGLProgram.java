package com.myvideoyun.shortvideo.GPUImage;

import android.util.Log;

import static android.opengl.GLES20.*;

/**
 * Created by 汪洋 on 2018/12/8.
 * Copyright © 2018年 myvideoyun. All rights reserved.
 */
public class MVYGLProgram {

    private int program;
    private int vertShader;
    private int fragShader;

    public MVYGLProgram(String vShaderString, String fShaderString) {
        vertShader = compileShader(GL_VERTEX_SHADER, vShaderString);

        fragShader = compileShader(GL_FRAGMENT_SHADER, fShaderString);

        program = glCreateProgram();

        glAttachShader(program, vertShader);
        glAttachShader(program, fragShader);
    }

    private int compileShader(int type, String shaderString) {
        int shader = glCreateShader(type);

        if (shader != 0) {
            glShaderSource(shader, shaderString);
            glCompileShader(shader);

            int[] status = new int[1];
            glGetShaderiv(shader, GL_COMPILE_STATUS, status,0);

            if (status[0] == 0) {
                Log.d(MVYGPUImageConstants.TAG, "shader complie error : " + glGetShaderInfoLog(shader));
                glDeleteShader(shader);
                shader = 0;
            }
        }

        return shader;
    }

    public int attributeIndex(String attributeName) {
        return glGetAttribLocation(program, attributeName);
    }

    public int uniformIndex(String uniformName) {
        return glGetUniformLocation(program, uniformName);
    }

    public boolean link() {
        glLinkProgram(program);

        int[] status = new int[1];

        glGetProgramiv(program, GL_LINK_STATUS, status, 0);
        if (status[0] != GL_TRUE) {
            Log.d(MVYGPUImageConstants.TAG, "link program error : " +glGetProgramInfoLog(program));
            if (vertShader > 0) {
                glDeleteShader(vertShader);
                vertShader = 0;
            }
            if (fragShader > 0) {
                glDeleteShader(fragShader);
                fragShader = 0;
            }

            return false;
        }

        return true;
    }

    public void use() {
        glUseProgram(program);
    }

    public void destroy() {
        if (vertShader > 0) {
            glDeleteShader(vertShader);
            vertShader = 0;
        }
        if (fragShader > 0) {
            glDeleteShader(fragShader);
            fragShader = 0;
        }
        if (program > 0) {
            glDeleteProgram(program);
            program = 0;
        }
    }
}

