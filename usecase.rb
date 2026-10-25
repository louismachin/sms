require 'net/http'
require 'uri'
require 'dotenv'

Dotenv.load('./.env')

def send_sms(who, text, token: ENV.fetch('SMS_TOKEN'))
    SMS_BASE_URL = ENV.fetch('SMS_BASE_URL', 'https://sms.louismachin.com')
    uri = URI("#{SMS_BASE_URL}/send/#{URI.encode_www_form_component(who)}")
    req = Net::HTTP::Post.new(uri)
    req['Authorization'] = "Bearer #{token}"
    req['Content-Type'] = 'text/plain; charset=utf-8'
    req.body = text.to_s
    res = Net::HTTP.start(
        uri.host, uri.port, 
        use_ssl: uri.scheme == 'https', open_timeout: 5, read_timeout: 15
    ) { |http| http.request(req) }
    return true if res.is_a?(Net::HTTPSuccess)
    warn "send_sms(#{who}) failed: HTTP #{res.code} #{res.body.to_s.strip}"
    false
rescue StandardError => e
    warn "send_sms(#{who}) failed: #{e.class}: #{e.message}"
    false
end

send_sms('louis', 'Hello World!')