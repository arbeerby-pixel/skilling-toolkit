# Skilling Toolkit

Skilling Toolkit brings practical skilling helpers and session tracking into one RuneLite plugin.

> **Development status**
>
> Skilling Toolkit is still in active development. If you find something that does not work as expected, or have an idea for a useful new feature, we would love to hear your feedback and suggestions.

## Community

Join the Arber Plugins Discord for feature suggestions, bug reports, support, and plugin updates:

https://discord.gg/utYem4XhQS

## Features

### Farming

- Grey and colored compost bucket markers show whether a patch still needs compost.
- Gardener payment checks mark protected crops on supported patches.
- Attas, Iasor, and Kronos timers show their remaining lifetime in hours and minutes.
- Saved patch state survives RuneLite restarts and can be reset from the plugin settings or sidebar.

### Hunter

- Seeded bird houses receive a check marker until they are ready or emptied.

### Trackers and interface

- Dynamic chat-tab trackers show XP, top skill, profit, cost, and actions in the order they become active.
- When PvM Toolkit is also enabled, the `All` chat tab switches manually between skilling and PvM trackers while activity can still switch them automatically.
- The Trade tab remains a local 24-hour clock.
- The inventory infobox shows free slots and inventory value.
- Sidebar statistics are available for day, week, month, year, and all time.
- Inventory value can use either Grand Exchange guide prices or RuneLite prices.

Skilling Toolkit coordinates its chat tabs and inventory infobox with PvM Toolkit when both plugins are enabled. Each plugin also works independently.

## Development

Java 11 is required. Start the RuneLite development client with:

```powershell
.\gradlew.bat run
```

## License

Skilling Toolkit is licensed under the BSD 2-Clause License. See `LICENSE`.
