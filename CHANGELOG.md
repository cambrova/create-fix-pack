# Changelog

## 1.1.0

- Empty weighted ejectors take more than one item at a time, so brass funnels set to a stack can feed them (Create #7681).

## 1.0.2

- New experimental switch `linkKeepsWaitingLinks` (off by default) for Create #5009: turn it on if Redstone Link receivers stop following their transmitter after chunks reload.

## 1.0.1

- The two #9987 fixes that have no test yet (singleplayer double count, packager briefly blind) are now separate experimental switches, off by default.

## 1.0.0

- Mechanical crafters keep their waiting result when the world loads (Create #4510).
- Factory gauges no longer order more than their target after items leave a linked inventory (Create #9987).
