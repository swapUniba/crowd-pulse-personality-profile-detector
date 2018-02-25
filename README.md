# crowd-pulse-personality-profile-detector
This plugin detects the user personality from all input messages. Personality is composed of 5 dimensions: openness, 
conscientiousness, extroversion, agreeableness, neuroticism. Each dimension varies in [0, 1] range.

Plugin configuration example:
```json
"personalityDetector": {
  "plugin": "personality-profile-detector",
  "config": {
    "profilesDatabaseName": "profiles",
    "username": "abcUserName"
  }
}
```
- **profilesDatabaseName**: the profiles database name to connect and retrieve the user profile entity;
- **username**: the username used to get the user profile from database.

Example of usage:
```json
{
  "process": {
    "name": "personality-tester",
    "logs": "/opt/crowd-pulse/logs"
  },
  "nodes": {
    "fetch": {
      "plugin": "message-fetch",
      "config": {
        "db": "test"
      }
    },
    "personalityDetector": {
      "plugin": "personality-profile-detector",
      "config": {
        "profilesDatabaseName": "profiles",
        "username": "{{dbName}}"
      }
    }
  },
  "edges": {
    "fetch": [
      "personalityDetector"
    ]
  }
}
```

**IMPORTANT**: the PersonalityDetector plugin has its own persistence mechanism, so you don't need the ProfilePersister
plugin.