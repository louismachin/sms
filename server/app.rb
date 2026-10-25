# sms.louismachin.com
#
#   POST /send/:who      Authorization: Bearer $SMS_TOKEN, body = plain text
#   POST /incoming       called by the phone (X-Sms-Secret: $SMS_SECRET)
#
# Env: SMS_TOKEN, SMS_SECRET, optional ADB_SERIAL

require 'sinatra'
require 'json'
require 'open3'
require 'shellwords'
require 'yaml'

require 'dotenv'
Dotenv.load(File.expand_path('../.env', __dir__))

set :port, Integer(ENV.fetch('PORT'))
set :bind, '127.0.0.1' # reverse proxy
set :environment, :production
disable :protection

PKG = 'com.louismachin.smsbridge'

contacts_path = File.expand_path('../contacts.yml', __dir__)
CONTACTS = File.file?(contacts_path) ? YAML.load_file(contacts_path).freeze : {}

def adb(*args)
    cmd = ['adb', *(ENV['ADB_SERIAL'] ? ['-s', ENV['ADB_SERIAL']] : []), *args]
    Open3.capture2e(*cmd)
end

# `adb shell` re-joins args and runs them through the phone's sh, so escape each one.
def adb_shell(*args) = adb('shell', args.map { Shellwords.escape(_1) }.join(' '))

def send_sms(number, text)
    out, status = adb_shell(
        'am', 'broadcast', '-f', '32', '-n', "#{PKG}/.SendReceiver",
        '--es', 'to', number,
        '--es', 'body', text
    )
    status&.success? && out.include?('result=200') ? [true, out] : [false, out]
end

# Phone's 127.0.0.1:4535 -> this server, over USB. Lost on phone reboot/replug.
configure { adb('reverse', "tcp:#{settings.port}", "tcp:#{settings.port}") }

def secure_eq?(a, b) = a && b && Rack::Utils.secure_compare(a, b)

require_relative './outbound'
require_relative './inbound'

get('/health') { adb('get-state').first.strip == 'device' ? "ok\n" : halt(503, "phone missing\n") }

not_found do
    halt 418
end