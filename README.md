# geminikit

The tools you need to explore the Geminispace.

I created this project as a way to serve a personal blog (glog) but my desire is that it evolves into a suite of tools to transition to Geminispace bringing along things I like in the bloated web.

## Roadmap

- [ ] Server (IN PROGRESS)
  - [ ] Ring-like req/rsp maps. (IN PROGRESS)
  - [ ] Ring-like middlewares.
  - [ ] Middleware for serving files from disk. (IN PROGRESS)
- [ ] Client
- [ ] Gemtext
  - [ ] HTML<->Gemtext converter
  - [ ] Markdowm<->Gemtext converter

## Server

A gemini server based on aleph with a ring-like req/rsp map model.

### Serving files

Check the examples folder on how to server files from your disk.
It's good to run a simple capsule based on statis files.
