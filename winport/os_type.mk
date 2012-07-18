# Set os_type to standard name for OS type.
os_type = linux
ifneq ($(findstring darwin,$(OSTYPE)),)
 os_type = mac
endif
ifneq ($(findstring cygwin,$(shell echo $$OSTYPE)),)  # Cygwin OSTYPE is shell var, not env var
 os_type = cygwin
endif
