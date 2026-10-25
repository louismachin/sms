COMMANDS = {
    'status' => -> { `uptime`.strip },
    'disk'   => -> { `df -h /`.lines.last.strip },
    'help'   => -> { "commands: #{COMMANDS.keys.join(', ')}" }
}.freeze

post '/incoming' do
    halt 401 unless secure_eq?(request.env['HTTP_X_BRIDGE_SECRET'], ENV.fetch('BRIDGE_SECRET'))
    msg = JSON.parse(request.body.read)
    from, body = msg.values_at('from', 'body')

    # Sender IDs can be spoofed: this check keeps out randoms, not a determined attacker.
    # Gate anything destructive behind a PIN as well.
    unless CONTACTS.value?(from)
        logger.warn "ignored text from #{from}"
        halt 204
    end

    cmd = COMMANDS[body.strip.downcase]
    reply = cmd ? cmd.call : "unknown command (try 'help')"
    Thread.new { send_sms(from, reply[0, 600]) } # don't block the phone's webhook call
    204
end