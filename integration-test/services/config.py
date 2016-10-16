class KafkaConfig(dict):
    """A dictionary-like container class which allows for definition of overridable default values,
    which is also capable of "rendering" itself as a useable server.properties file.
    """
    DEFAULTS = {
        PORT: 9092,
        LOG_DIRS: "/tmp/kafka-data-logs",
        ZOOKEEPER_CONNECTION_TIMEOUT_MS: 2000
    }

    def __init__(self, **kwargs):
        super(KafkaConfig, self).__init__(**kwargs)

        # Set defaults
        for key, val in self.DEFAULTS.items():
            if not self.has_key(key):
                self[key] = val

    def render(self):
        """Render self as a series of lines key=val\n, and do so in a consistent order. """
        keys = [k for k in self.keys()]
        keys.sort()

        s = ""
        for k in keys:
            s += "%s=%s\n" % (k, str(self[k]))
        return s
