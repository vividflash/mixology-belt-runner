package com.mixologybeltrunner;

public enum TrackerState
{
	/**
	 * Every potion in the inventory is accounted for.
	 */
	TRACKING,
	/**
	 * Tracking stopped; it resumes once the inventory holds no mixology potion.
	 */
	NEEDS_CLEAR
}
