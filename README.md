# quick-diff

[![Build Status](https://travis-ci.org/obruchez/quick-diff.svg?branch=master)](https://travis-ci.org/obruchez/quick-diff)

Quickly find differences between two directories.

## Synopsis

QuickDiff [--check-dates] [--full-diff] src-path dst-path

## Description

QuickDiff is a tool to quickly compare the contents of two paths. It is "quick" in the sense that, by default, the actual contents of the files is not checked; only the name of the files and their size is taken into account. An option can be added to check the modification dates.

The differences are reported in several groups:

* files that are different (i.e. that have different sizes, different modification dates, or, in "full diff" mode, different contents)
* out-of-date files found in the source path (i.e. files that are also present in the destination path, but with a more recent modification date)
* files missing from the source path
* directories missing from the source path
* out-of-date files found in the destination path (i.e. files that are also present in the source path, but with a more recent modification date)
* files missing from the destination path
* directories missing from the destination path

QuickDiff can typically be helpful to quickly check which files/directories in a source path would need to be synced/mirrored to a destination path using tools such as rsync or rsync-backup. It only works locally, though, but displays a slightly more detailed report of the differences than rsync in --dry-run mode, for example.

## Options

--check-dates

By default, same-size files are considered identical and different-size files are considered different. If the "check dates" mode is enabled, files must have the same size but also the same modification date to be considered identical.

--full-diff

By default, files are considered identical if they have the same size (and, in "check dates" mode, if they have the same modification date). In "full diff" mode, the actual contents of the files is also compared. The actual differences between the files is not displayed, though. Contrary to some diff tools, this option allows the comparison of "big" files (typically, files that are bigger than the available memory).

## To-do

* add option to enable recursion (recursion mode now enabled by default)
* implement full diff mode (--full-diff option)
