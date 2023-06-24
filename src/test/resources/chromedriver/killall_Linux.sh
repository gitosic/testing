#!/bin/bash
ps -a | grep chrome
pgrep chrome | xargs kill -9
rm -rf /temp/.com.google*