package jp.co.njr.nju9101demo;

public abstract class State<T> {
    public T argument;
    public abstract State getNextState();
    public abstract void execute();
}
