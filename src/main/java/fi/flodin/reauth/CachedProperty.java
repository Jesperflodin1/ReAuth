package fi.flodin.reauth;

final class CachedProperty<T> {
	private T value;
	private final T invalid;
	private boolean valid;

	private long timestamp;
	private final long validity;

	CachedProperty(final long validity, final T invalid) {
		this.validity = validity;
		this.invalid = invalid;
	}

	boolean check() {
		if (System.currentTimeMillis() - timestamp > validity) {
			invalidate();
		}
		return valid;
	}

	T get() {
		return valid ? value : invalid;
	}

	void invalidate() {
		valid = false;
	}

	void set(final T value) {
		this.value = value;
		this.timestamp = System.currentTimeMillis();
		this.valid = true;
	}

	long timestamp() {
		return timestamp;
	}
}
