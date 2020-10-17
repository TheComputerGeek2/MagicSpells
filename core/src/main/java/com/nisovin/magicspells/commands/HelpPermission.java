package com.nisovin.magicspells.commands;

import com.nisovin.magicspells.Perm;

import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface HelpPermission {

	Perm permission();

}
