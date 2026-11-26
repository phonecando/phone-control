# Security

Phone control is the part of PhoneCanDo that lets AI agents see and use a
phone, so a hole here matters. Thank you for looking.

## Reporting a vulnerability

Please report it privately, never in a public issue, pull request or
discussion:

1. Open a private security advisory on this repository: **Security →
   Report a vulnerability**
   (<https://github.com/phonecando/phone-control/security/advisories/new>).
2. If you can't use GitHub, write to hi@phonecando.app with "Security" in the
   subject.

Tell us what you found, the app version and build (full, lite or play) or the
commit it is in, your phone and Android version, and the steps to show it. A
proof of concept helps; please keep it to your own phone and accounts.

## What happens next

- We reply within 7 days to say we have it.
- We keep you posted while we fix it, and agree with you when to publish.
- Once the fix ships in a release, we publish the advisory and credit you,
  unless you'd rather we didn't.

## Scope

Anything that lets an agent, an app or anyone else get past phone control's
gates is in scope: acting without the control lock armed, reaching an app you
haven't allowed, using the screen while you hold the controls or while another
agent has it, reaching the bridge without an agent's key, or taking a key
that isn't yours. So is anything in `engine/` or `tools/` that reads, sends or
does more than its docs say.

The rest of the PhoneCanDo app is closed source, but we want those reports
too: report them privately on
[phonecando/feedback](https://github.com/phonecando/feedback/security/advisories/new),
or here if you aren't sure which side a hole is on.

## Supported versions

We fix the latest release. Update the app before you report, if you can.
