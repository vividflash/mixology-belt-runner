package com.mixologybeltrunner;

public enum WindowStyle
{
	NEXT_STEP("Next step"),
	TODO_LIST("To-do list"),
	FULL_GRID("Full grid");

	private final String label;

	WindowStyle(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
